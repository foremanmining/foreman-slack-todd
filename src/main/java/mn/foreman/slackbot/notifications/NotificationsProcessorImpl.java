package mn.foreman.slackbot.notifications;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;
import mn.foreman.api.endpoints.notifications.Notifications;
import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link NotificationsProcessor} implementation that sends
 * markdown-formatted messages to the provided chat based on the session that's
 * to be notified.
 */
@Component
public class NotificationsProcessorImpl
        implements NotificationsProcessor {

    /** The logger for this class */
    private static final Logger LOG =
            LoggerFactory.getLogger(NotificationsProcessorImpl.class);

    /** The {@link App} to talk to Slack. */
    private final App app;

    /** Base URl for Foreman */
    private final String foremanDashboardUrl;

    /** The max notifications to send at once. */
    private final int maxNotifications;

    /** The mapper. */
    private final ObjectMapper objectMapper;

    /** The bot start time. */
    private final Instant startTime;

    /**
     * Constructor for {@link NotificationsProcessorImpl}.
     *
     * @param foremanDashboardUrl the actual dashboard for the user
     * @param maxNotifications    max number of notifications a user will
     *                            receive at once, currently set at 10
     * @param objectMapper        the mapper
     * @param startTime           this is the time when the user registered
     * @param app                 The Slack API.
     */
    public NotificationsProcessorImpl(
            @Value("${foreman.baseUrl}") final String foremanDashboardUrl,
            @Value("${notifications.max}") final int maxNotifications,
            final ObjectMapper objectMapper,
            final Instant startTime,
            final App app) {
        this.foremanDashboardUrl = foremanDashboardUrl;
        this.maxNotifications = maxNotifications;
        this.objectMapper = objectMapper;
        this.startTime = startTime;
        this.app = app;
    }

    @Override
    public void process(
            final State state,
            final StateRepository stateRepository) {
        // Gives us the channel that the user is working on
        final String channelId = state.getChatId();

        final ForemanApi foremanApi =
                makeApi(
                        state);

        final Notifications notificationsApi =
                foremanApi.notifications();

        //this is the time the user enters the register command in a particular channel
        final Instant registered = state.getDateRegistered();

        //check the notification time against the time the user registered
        final List<Notifications.Notification> notifications =
                notificationsApi.slack(
                        state.getLastNotificationId(),
                        registered.isAfter(this.startTime)
                                ? registered
                                : this.startTime);

        LOG.info("Session {} has {} pending notifications",
                state,
                notifications);
        if (!notifications.isEmpty()) {
            sendExistingNotifications(
                    stateRepository,
                    state,
                    channelId,
                    notifications
            );
        }
    }

    /**
     * Adds the miners information and link to the notification sent
     *
     * @param failingMiner  this is the individual miner that failed for the
     *                      user on foreman
     * @param stringBuilder java string builder
     */
    private void appendMiner(
            final Notifications.Notification.FailingMiner failingMiner,
            final StringBuilder stringBuilder) {
        stringBuilder
                .append(
                        String.format(
                                // If there is an issue with the output,
                                // this is where it would most likely be
                                "<%s/dashboard/miners/%d/details/|%s>",
                                this.foremanDashboardUrl,
                                failingMiner.minerId,
                                failingMiner.miner))
                .append("\n");
        failingMiner
                .diagnosis
                .forEach(
                        diag ->
                                stringBuilder
                                        .append(diag)
                                        .append("\n"));
        stringBuilder
                .append("\n");
    }

    /**
     * Creates the foreman Api Url This is the same as lines 95-104 in foreman
     * discord NotificationsProcessorImpl just done in a separate method
     *
     * @return A new {@link ForemanApi} authenticated with the data from the
     *         provided {@link State}.
     */
    private ForemanApi makeApi(final State state) {
        // initializing the client Id
        final String clientId = Integer.toString(state.getClientId());

        // returning the desired ForemanApiImpl object
        return new ForemanApiImpl(
                clientId,
                "",
                this.objectMapper,
                new JdkWebUtil(
                        this.foremanDashboardUrl,
                        state.getApiKey(),
                        5,
                        TimeUnit.SECONDS));
    }

    /**
     * This method builds a string of notifications and sends it to the user's
     * slack channel
     *
     * @param stateRepository The repository.
     * @param state           The current {@link State} for the user.
     * @param channelId       The channel ID.
     * @param notifications   The actual notification that is being sent to the
     *                        user.
     */
    private void sendExistingNotifications(
            final StateRepository stateRepository,
            final State state,
            final String channelId,
            final List<Notifications.Notification> notifications) {
        LOG.info("Building notification message for {}", state);
        notifications
                .stream()
                .map(this::toNotificationMessage)
                .forEach(message -> {
                    try {
                        sendMessage(
                                state,
                                channelId,
                                message);
                    } catch (final Exception e) {
                        LOG.warn("Exception occurred while notifying", e);
                    }
                });

        final Notifications.Notification lastNotification =
                Iterables.getLast(notifications);
        state.setLastNotificationId(lastNotification.id);
        stateRepository.save(state);
    }

    /**
     * Method for sending messages to slack. More details can be found at:
     *
     * <p>
     * More info <a
     * href="https://slack.dev/java-slack-sdk/guides/web-api-basics">here</a>
     * </p>
     *
     * @param state     this is the {@link State} for the user and their session
     * @param channelId the users channel Id
     * @param message   this is the actual text that's being sent
     *
     * @throws IOException on failure.
     */
    private void sendMessage(
            final State state,
            final String channelId,
            final String message)
            throws Exception {
        try (final Slack slack = Slack.getInstance()) {
            final String botToken= state.getBotToken();
            final MethodsClient methods = slack.methods(botToken);

            // Chat post message requires try catch block and the above exceptions
            // this is handled in the method call
            methods.chatPostMessage(
                    ChatPostMessageRequest
                            .builder()
                            .channel(channelId)
                            .text(message)
                            .build());
        }
    }

    /**
     * Converts the provided notification to a Slack message to be sent.
     *
     * @param notification The notification to process.
     *
     * @return The Slack, markdown-formatted message.
     */
    private String toNotificationMessage(
            final Notifications.Notification notification) {
        final StringBuilder messageBuilder =
                new StringBuilder();

        //write the subject
        messageBuilder.append(
                String.format(
                        "%s *%s*",
                        !notification.failingMiners.isEmpty()
                                ? ":x:"
                                : ":white_check_mark:",
                        notification.subject));

        final List<Notifications.Notification.FailingMiner> failingMiners =
                notification.failingMiners;

        if (!failingMiners.isEmpty()) {
            // Write the failing miners out as lists
            messageBuilder.append("\n\n");
            failingMiners
                    .stream()
                    .limit(this.maxNotifications)
                    .forEach(
                            miner ->
                                    appendMiner(
                                            miner,
                                            messageBuilder));

            if (failingMiners.size() > this.maxNotifications) {
                // Too many miners were failing if we get here
                messageBuilder
                        .append("\n\n")
                        .append(
                                String.format(
                                        "*...and %d more",
                                        failingMiners.size() - this.maxNotifications))

                        .append(
                                String.format(
                                        "Head to [your dashboard](%s/dashboard/) to see the rest",
                                        this.foremanDashboardUrl));
            }
        }

        return messageBuilder.toString();
    }
}
