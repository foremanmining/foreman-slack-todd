package mn.foreman.slackbot.handlers;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;

/**
 * This command gives the user information about how to get started with using
 * the bot and how to proceed with the register command
 */
public class StartCommandHandler
        implements SlashCommandHandler {

    /** The Foreman dashboard URL */
    private final String foremanDashboardUrl;

    /** Constructor for the start command */
    public StartCommandHandler(final String foremanDashboardUrl) {
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public Response apply(
            final SlashCommandRequest slashCommandRequest,
            final SlashCommandContext context) {
        return context.ack(
                "Hello! I'm *Todd*, the Foreman Slack notification bot. :wave: \n" +
                        "\n" +
                        String.format(
                                "Based on <%s/dashboard/triggers/|triggers> you create on your dashboard, I'll send you notifications when things happen.\n",
                                this.foremanDashboardUrl) +
                        "\n" +
                        "Let's get introduced:\n" +
                        "\n" +
                        String.format(
                                "1. Go <%s/dashboard/profile/|here> get your *client id* and *API key*\n",
                                this.foremanDashboardUrl) +
                        "2. Once you have them, run: `/register <client_id> <api_key>`\n" +
                        "3. That's it! :beers: Then I'll send your notifications to this channel.\n" +
                        "\n" +
                        "If you want them to happen somewhere else, re-run the register above in the channel where you want to be notified.");
    }
}
