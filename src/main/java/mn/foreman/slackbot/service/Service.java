package mn.foreman.slackbot.service;

import com.slack.api.bolt.Initializer;

/** Service interface */
public interface Service {
    /**
     * Returns the initializer for this service. If the service has time-consuming initialization steps,
     *  putting those into this function is a good way to avoid timeout errors
     *  for the first incoming request
     */
    default Initializer initializer() {
        return (app) -> {
        };
    }
}
