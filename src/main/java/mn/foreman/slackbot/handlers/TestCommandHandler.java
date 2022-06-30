package mn.foreman.slackbot.handlers;

import mn.foreman.api.ForemanApi;
import mn.foreman.api.endpoints.ping.Ping;
import mn.foreman.slackbot.config.ForemanUtils;
import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;

import java.util.Optional;

/** This lets the user test their connectivity to the foreman api */
public class TestCommandHandler implements SlashCommandHandler {

    /** URl for Foreman Api */
    private final String foremanApiUrl;

    /** Where {@link State}s are stored. */
    private final StateRepository stateRepository;

    /**
     * Constructor for the test command
     *
     * @param stateRepository Where {@link State}s are stored.
     * @param foremanApiUrl the Url for the Foreman Api
     */
    public TestCommandHandler(
            final StateRepository stateRepository,
            final String foremanApiUrl) {
        this.stateRepository = stateRepository;
        this.foremanApiUrl = foremanApiUrl;
    }

    @Override
    public Response apply(SlashCommandRequest slashCommandRequest, SlashCommandContext context) {
        final String channelId = context.getChannelId();

        final StringBuilder messageBuilder = new StringBuilder();

        final Optional<State> stateOpt = this.stateRepository.findById(channelId);
        if (stateOpt.isPresent()) {
            final State state = stateOpt.get();

            final ForemanApi foremanApi = ForemanUtils.toApi(state.getClientId(), state.getApiKey(), this.foremanApiUrl);
            final Ping ping = foremanApi.ping();

            if (ping.ping()) {
                messageBuilder.append("*Connectivity to Foreman:* :white_check_mark:\n");
            } else {
                messageBuilder.append("*Connectivity to Foreman:* :x:\n");
            }

            if (ping.pingClient()) {
                messageBuilder.append("*Authentication with your API credentials:* :white_check_mark:\n");
            } else {
                messageBuilder.append("*Authentication with your API credentials:* :x:\n");
            }
        } else {
            messageBuilder.append("We haven't met yet...");
        }

        return context.ack(messageBuilder.toString());
    }
}
