package mn.foreman.slackbot.notifications;

import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;


/** This class works to send notifications to the user. */
@Component
public class Notifier {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(Notifier.class);

    /** The app. */
    private final App app;

    /**
     * This is the Api url for the user. Its important to note that this is
     * distinct from the foreman dashboard URL
     */
    private final String foremanApiUrl;

    /** The max number of notifications the user will receive at once */
    @Value("${notifications.max}")
    private final int maxNotifications;

    /** the mapper */
    private final ObjectMapper objectMapper;

    /**
     * The time that the first notification is set off. Distinct from the time
     * that the user registers.
     */
    private final Instant startTime;

    /** This is the state repository used to help maintain state/session */
    private final StateRepository stateRepository;

    /**
     * This is the constructor for the notifier. It calls
     * {@link NotificationsProcessorImpl}
     *
     * @param startTime        the time that notification occurs
     * @param stateRepository  the backing {@link State} repository
     * @param objectMapper     the mapper
     * @param foremanApiUrl    the url for the Api
     * @param maxNotifications max number of notifications to send at once can
     *                         be increased or decreased as desired. Setting to
     *                         0 is not advised
     * @param app              The app.
     */
    public Notifier(
            final Instant startTime,
            final StateRepository stateRepository,
            final ObjectMapper objectMapper,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${notifications.max}") final int maxNotifications,
            final App app) {
        this.startTime = startTime;
        this.stateRepository = stateRepository;
        this.objectMapper = objectMapper;
        this.foremanApiUrl = foremanApiUrl;
        this.maxNotifications = maxNotifications;
        this.app = app;
    }

    /** Periodically sends notifications to the users. */
    @Scheduled(
            initialDelayString = "${bot.check.initialDelay}",
            fixedDelayString = "${bot.check.fixedDelay}")
    public void sendNotifications() {
        final List<State> states =
                this.stateRepository.findAll();
        final NotificationsProcessor notificationProcessor =
                new NotificationsProcessorImpl(
                        this.foremanApiUrl,
                        this.maxNotifications,
                        this.objectMapper,
                        this.startTime,
                        this.app);
        LOG.info("Looking for notifications for {} sessions", states.size());
        // makes sure the list of states is non-empty
        if (!states.isEmpty()) {
            states
                    .parallelStream()
                    .forEach(
                            state ->
                                    notificationProcessor.process(
                                            state,
                                            this.stateRepository));
        }
    }
}

