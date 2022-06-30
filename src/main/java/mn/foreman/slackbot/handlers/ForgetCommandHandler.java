package mn.foreman.slackbot.handlers;

import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;

import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;

/** Makes the bot remove a channel so that it doesn't send alerts there anymore */
public class ForgetCommandHandler implements SlashCommandHandler {

    /** Where {@link State}s are stored. */
    private final StateRepository stateRepository;

    /**
     * The constructor for this command
     *
     * @param stateRepository The backing {@link State} repository.
     */
    public ForgetCommandHandler(final StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @Override
    public Response apply(
            final SlashCommandRequest slashCommandRequest,
            final SlashCommandContext context) {
        // What the function will be outputting to the user
        final String output;

        // Gets the channel ID
        final String channelId = context.getChannelId();

        // If it is present its deleted and a response is sent out indicating
        // that
        if (this.stateRepository.findById(channelId).isPresent()) {
            this.stateRepository.deleteById(channelId);
            output = "Got it - I won't send you notifications anymore";
        } else {
            // In this case they haven't done the response step yet.
            output = "I don't think we've met...";
        }
        
        return context.ack(output);
    }
}
