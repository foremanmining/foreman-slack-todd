package mn.foreman.slackbot.notifications;

import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;


/**
 * A {@link NotificationsProcessor} provides a mechanism for obtaining pending
 * Slack notifications for a chat via the Foreman API and sends notifications to
 * a chat accordingly.
 */
public interface NotificationsProcessor {

    /**
     * Obtains notifications for the provided session and notifies the chat, as
     * necessary.
     *
     * @param state Provides the users credentials.
     */
    void process(
            State state,
            StateRepository stateRepository);
}
