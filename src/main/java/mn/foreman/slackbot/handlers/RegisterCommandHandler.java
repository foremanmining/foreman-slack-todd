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
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * This creates a state that the bot uses in order to send notifications to
 * the users slack channel the if the values for either client Id or Api key
 * are incorrect then the bot says so
 */
public class RegisterCommandHandler implements SlashCommandHandler {

    /** Logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(RegisterCommandHandler.class);

    /** URl for Foreman API. */
    private final String foremanApiUrl;

    /** URL for foreman dashboard */
    private final String foremanDashboardUrl;

    /** This is the mongo repository for this session state. */
    private final StateRepository stateRepository;

    /** When the state/session was added. */
    private Instant dateRegistered;

    /**
     * Constructor for this class
     *
     * @param stateRepository     The repository.
     * @param foremanApiUrl       The Foreman API base URL.
     * @param foremanDashboardUrl The dashboard URL.
     */
    public RegisterCommandHandler(
            final StateRepository stateRepository,
            final String foremanApiUrl,
            final String foremanDashboardUrl) {
        this.stateRepository = stateRepository;
        this.foremanApiUrl = foremanApiUrl;
        this.foremanDashboardUrl = foremanDashboardUrl;
    }

    @Override
    public Response apply(
            final SlashCommandRequest slashCommandRequest,
            final SlashCommandContext context) {

        // From slack to get the client id and API
        final String arguments = slashCommandRequest.getPayload().getText();

        // String for the output that will be returned
        String output = " ";

        //Split on spaces,
        // get first (client ID), get second (API key),
        // split the string on spaces and handle the < and > characters.
        // the >< handles the case where the client doesn't include a space between their client Id and the Api key
        if (arguments != null && !arguments.isBlank()) {
            final String[] splitArgs =
                    arguments.replace("><", " ")
                            .replace("<", "")
                            .replace(">", "")
                            .split(" ");
            final String channelId = context.getChannelId();

            // Re-registering - clear the old
            if (this.stateRepository.findById(channelId).isPresent()) {
                this.stateRepository.deleteById(channelId);
            }

            if (splitArgs.length >= 2) {
                output += applyValidArguments(
                        context,
                        splitArgs);
            } else {
                output += "Sorry something isn't right. You should input <clientId> followed by <API key>";
            }
        } else {
            output += "Sorry something isn't right. You should input <clientId> followed by <API key>";
        }
        return context.ack(output);

    }

    /**
     * This method checks that the users credentials were correct/correctly
     * input.
     *
     * @param context  information about the user from their slack information
     * @param splitArgs the users client Id and Api key
     *
     * @return a string containing a verification if the input is valid. If
     *         not then the string says what went wrong
     */
    private String applyValidArguments(
            final SlashCommandContext context,
            final String[] splitArgs) {

        String outPutArgs = " ";
        final String clientIdCandidate = splitArgs[0];
        // Here we do a quick check to ensure that the client Id is a number before moving on.
        if (NumberUtils.isCreatable(clientIdCandidate)) {
            final int clientId = Integer.parseInt(clientIdCandidate);
            final String apiKey = splitArgs[1];

            final ForemanApi foremanApi = ForemanUtils.toApi(clientId, apiKey, this.foremanApiUrl);
            final Ping ping = foremanApi.ping();
            if (ping.pingClient()) {

                // Does the same thing as handle success in discord adds state and sends the user a confirmation message
                // Builds the state repository and adds the client Id, Api Key and channel Id to the session/state
                this.stateRepository.insert(
                        State
                                .builder()
                                .dateRegistered(Instant.now())
                                .clientId(clientId)
                                .apiKey(apiKey)
                                .chatId(context.getChannelId())
                                .build());

                // Concatenate the confirmation text
                outPutArgs += ("Those look correct! Setup complete :white_check_mark:\n" +
                        "\n" +
                        String.format("You'll get notified based on your *alert* <%s/dashboard/triggers/|triggers>, " +
                                "so make sure you created some and set their destination to" +
                                "Slack. \n", this.foremanDashboardUrl) +
                        "\n" +
                        "If you've already done this, you should be good to go! :thumbsup:");
            } else {
                outPutArgs += ("I tried those, but they didn't work. Please re-input the register command followed " +
                        "by your client Id and api key to try again");
            }
        } else {
            //Log the invalid number and return a message to the user telling them their input was wrong
            LOG.warn("Number not provided: {}", clientIdCandidate);
            outPutArgs += "Sorry, client ID should have been a number. Please try again.";
        }
        return outPutArgs;
    }
}
