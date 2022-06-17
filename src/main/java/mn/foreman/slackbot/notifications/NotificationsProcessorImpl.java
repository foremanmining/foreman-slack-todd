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
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
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

    /** Base URl for Foreman */
    private final String foremanDashboardUrl;

    /** The token for the app */
    private final String appToken;

    /** The max notifications to send at once. */
    private final int maxNotifications;

    /** The mapper. */
    private final ObjectMapper objectMapper;

    /** The bot start time. */
    private final Instant startTime;

    /** User information for their session which allows us to maintain state */
    private final StateRepository stateRepository;

    /**
     * Constructor for {@link NotificationsProcessorImpl}.
     *
     * @param foremanDashboardUrl the actual dashboard for the user
     * @param stateRepository     holds all of our data for maintaining state
     * @param maxNotifications    max number of notifications a user will
     *                            receive at once, currently set at 10
     * @param objectMapper        the mapper
     * @param startTime           this is the time of
     */
    public NotificationsProcessorImpl(
            @Value("${foreman.baseUrl}") final String foremanDashboardUrl,
            final StateRepository stateRepository,
            @Value("${notifications.max}") final int maxNotifications,
            final ObjectMapper objectMapper,
            final Instant startTime,
            @Value("${credentials.botToken}") final String appToken
            ) {
        this.foremanDashboardUrl = foremanDashboardUrl;
        this.stateRepository = stateRepository;
        this.maxNotifications = maxNotifications;
        this.objectMapper = objectMapper;
        this.startTime = startTime;
        this.appToken= appToken;
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
                                //if there is an issue with the output
                                //this is where it would be
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

    /** This method builds a string of notifications and sends it to the user's
     *  slack channel
     *
     * @param state the current {@link State} for the user
     * @param stateRepository The backing {@link State} repository
     * @param notifications the actual notification that is being sent to the user
     */
    private void sendExistingNotifications(
            StateRepository stateRepository,
            State state,
            String channelId,
            List<Notifications.Notification> notifications) {

        LOG.info("Building notification message for {}", state);
        notifications
                .stream()
                .map(this::toNotificationMessage)
                .forEach(message -> {
                    try {
                        sendMessage(
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
     * This is a method for sending messages to slack. More details can be
     * found at:
     *
     * <p>
     * More info <a
     * href="https://slack.dev/java-slack-sdk/guides/web-api-basics">here</a>
     * </p>
     *
     * @param channelId the users channel Id
     * @param message   this is the actual text that's being sent
     *
     * @return returns a slack message
     *
     * @throws IOException on failure.
     */
    private ChatPostMessageResponse sendMessage(
            final String channelId,
            final String message)
            throws SlackApiException, IOException {
        Slack slack = Slack.getInstance();

        String token = this.appToken;

        MethodsClient methods = slack.methods(token);

        // Building the request object
        ChatPostMessageRequest request = ChatPostMessageRequest
                .builder()
                .channel(channelId)
                .text(message)
                .build();
        // Chat post message requires try catch block and the above exceptions
        // this is handled in the method call
        ChatPostMessageResponse response = methods.chatPostMessage(request);

        return response;
    }

    /**
     *  Converts the provided notification to a Slack message to be sent.
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
