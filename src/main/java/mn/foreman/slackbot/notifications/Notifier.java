package mn.foreman.slackbot.notifications;

import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    /** This is the Api url for the user. Its important to note that this is
     * distinct from the foreman dashboard URL */
    private final String foremanApiUrl;

    /** The max number of notifications the user will receive at once */
    @Value("${notifications.max}")
    private final int maxNotifications;

    /** the mapper */
    private final ObjectMapper objectMapper;

    /** The token for the app */
    private final String appToken;

    /** The time that the first notification is set off. Distinct from the time that the user registers. */
    private final Instant startTime;


    /** This is the state repository used to help maintain state/session */
    private final StateRepository stateRepository;

    /**
     * This is the constructor for the notifier. It calls {@link NotificationsProcessorImpl}
     *
     * @param startTime        the time that notification occurs
     * @param stateRepository  the backing {@link State} repository
     * @param objectMapper     the mapper
     * @param foremanApiUrl    the url for the Api
     * @param maxNotifications max number of notifications to send at once
     *                         can be increased or decreased as desired.
     *                         Setting to 0 is not advised
     * @param appToken         token for the app. For OBM it is the slack bot
     */
    public Notifier(
            final Instant startTime,
            final StateRepository stateRepository,
            final ObjectMapper objectMapper,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${notifications.max}") final int maxNotifications,
            @Value("${credentials.botToken}") final String appToken) {
        this.startTime = startTime;
        this.stateRepository = stateRepository;
        this.objectMapper = objectMapper;
        this.foremanApiUrl = foremanApiUrl;
        this.maxNotifications = maxNotifications;
        this.appToken = appToken;
    }

    @Scheduled(fixedRate = 1000)
    public void sendNotifications() {
        final List<State> states =
                stateRepository.findAll();
        final NotificationsProcessor notificationProcessor =
                new NotificationsProcessorImpl(
                        foremanApiUrl,
                        stateRepository,
                        maxNotifications,
                        objectMapper,
                        startTime,
                        appToken
                        );
        LOG.info("Looking for notifications for {} sessions", states.size());
        // makes sure the list of states is non-empty
        if (!states.isEmpty()) {
            states
                    .parallelStream()
                    .forEach(
                            state ->
                                    notificationProcessor.process(
                                            state,
                                            stateRepository));
        }
    }
}

