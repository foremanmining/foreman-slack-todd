package mn.foreman.slackbot.handlers;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;

/** This command lets the user ask the bot/app what commands are available.
 *  The bot responds with the available commands as well as a corresponding
 *  description of each
 */
public class HelpCommandHandler
        implements SlashCommandHandler {

    @Override
    public Response apply(
            final SlashCommandRequest slashCommandRequest,
            final SlashCommandContext context) {
        return context.ack(
                "Sure... here's what I can do for ya:\n\n" +
                        "*/start:*\n" +
                        "Begins the bot setup process.\n\n" +
                        "*/register:*\n" +
                        "Registers the bot with new API credentials. Notifications will be sent to the channel where the registration was performed.\n\n" +
                        "*/forget:*\n" +
                        "Stops the bot from notifying you.\n\n" +
                        "*/test:*\n" +
                        "Tests connectivity with the Foreman API.");
    }

}
