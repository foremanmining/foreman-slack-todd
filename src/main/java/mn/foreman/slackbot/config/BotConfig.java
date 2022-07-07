package mn.foreman.slackbot.config;

import mn.foreman.slackbot.db.session.State;
import mn.foreman.slackbot.db.session.StateRepository;
import mn.foreman.slackbot.handlers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import com.slack.api.bolt.service.InstallationService;
import com.slack.api.bolt.service.OAuthStateService;
import com.slack.api.bolt.service.builtin.FileInstallationService;
import com.slack.api.bolt.service.builtin.FileOAuthStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * The two parts of this app are the oauth to allow other users to install this
 * bot followed by the functionality for the slash commands on Slack for now we
 * have start, register, forget, test, and help commands available.
 */
@Configuration
public class BotConfig {

    /**
     * This is the handler for the forget command
     *
     * @param stateRepository the repository where {@link State}s are stored.
     *
     * @return returns the forget command
     */
    @Bean
    public SlashCommandHandler forgetHandler(final StateRepository stateRepository) {
        return new ForgetCommandHandler(stateRepository);
    }

    /**
     * This is the handler for the help command
     *
     * @return returns the help command
     */
    @Bean
    public SlashCommandHandler helpHandler() {
        return new HelpCommandHandler();
    }

    /**
     * Creates the {@link App}.
     *
     * @param signingSecret      The slack API signing secret.
     * @param clientId           The slack API client ID.
     * @param clientSecret       The slack API client secret.
     * @param oAuthInstallPath   The slack API install path.
     * @param oAuthRedirectPath  The slack API redirect path.
     * @param scope              The bot scope.
     * @param oAuthCompletionUrl The completion URL.
     * @param rootDirectory      The path where states are stored.
     * @param startHandler       handles the start command.
     * @param registerHandler    handles the register command.
     * @param forgetHandler      handles the forget command.
     * @param testHandler        handles the test command.
     * @param helpHandler        handles the help command.
     *
     * @return The new {@link App}.
     */
    @Bean
    public App initSlackApp(
            @Value("${bot.credentials.signingSecret}") final String signingSecret,
            @Value("${bot.credentials.clientId}") final String clientId,
            @Value("${bot.credentials.clientSecret}") final String clientSecret,
            @Value("${bot.oauth.installPath}") final String oAuthInstallPath,
            @Value("${bot.oauth.redirectUriPath}") final String oAuthRedirectPath,
            @Value("${bot.scope}") final String scope,
            @Value("${bot.oauth.completionUrl}") final String oAuthCompletionUrl,
            @Value("${bot.oauth.cancellationUrl}") final String oAuthCancellationUrl,
            @Value("${bot.rootDir}") final String rootDirectory,
            final SlashCommandHandler startHandler,
            final SlashCommandHandler registerHandler,
            final SlashCommandHandler forgetHandler,
            final SlashCommandHandler testHandler,
            final SlashCommandHandler helpHandler) {

        // This handles the installation and Oauth. The singleTeamBotToken is
        // set to null since this app will be used on multiple work spaces
        // the oautCompletionUrl directs the user back to foreman
        final AppConfig appConfig =
                AppConfig
                        .builder()
                        .singleTeamBotToken(null)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .signingSecret(signingSecret)
                        .scope(scope)
                        .oAuthInstallPageRenderingEnabled(false)
                        .oauthInstallPath(oAuthInstallPath)
                        .oauthRedirectUriPath(oAuthRedirectPath)
                        .oauthCompletionUrl(oAuthCompletionUrl)
                        .oauthCancellationUrl(oAuthCancellationUrl)
                        .build();

        // Default installation and oauth service from slack. Creates and
        // stores the files locally under the users profile under the file
        // .slack-app
        final InstallationService installationService =
                new FileInstallationService(
                        appConfig,
                        rootDirectory);
        installationService.setHistoricalDataEnabled(true);

        final OAuthStateService stateService =
                new FileOAuthStateService(
                        appConfig,
                        rootDirectory);

        final App app =
                new App(appConfig)
                        .asOAuthApp(true);
        app.service(installationService);
        app.service(stateService);

        // these handle the slash commands associated with the bot
        app.command(
                "/start",
                startHandler);
        app.command(
                "/register",
                registerHandler);
        app.command(
                "/forget",
                forgetHandler);
        app.command(
                "/test",
                testHandler);
        app.command(
                "/help",
                helpHandler);

        return app;
    }

    /**
     * Returns a new JSON {@link ObjectMapper}.
     *
     * @return The mapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    /**
     * Allows the user to register to get notifications and notifies them of
     * success or failure
     *
     * @param stateRepository     the repository where {@link State}s are
     *                            stored.
     * @param foremanApiUrl       the Url for the Foreman Api
     * @param foremanDashboardUrl the Url for the Foreman Dashboard
     *
     * @return returns the register command
     */
    @Bean
    public SlashCommandHandler registerHandler(
            final StateRepository stateRepository,
            @Value("${foreman.apiUrl}") final String foremanApiUrl,
            @Value("${foreman.baseUrl}") final String foremanDashboardUrl) {
        return new RegisterCommandHandler(
                stateRepository,
                foremanApiUrl,
                foremanDashboardUrl);
    }

    /**
     * Gives the user an introduction and directions on how to begin
     *
     * @param foremanDashboardUrl the Url for the Foreman Dashboard
     *
     * @return returns the start command
     */
    @Bean
    public SlashCommandHandler startHandler(
            @Value("${foreman.baseUrl}") final String foremanDashboardUrl) {
        return new StartCommandHandler(foremanDashboardUrl);
    }

    /**
     * Returns the application start time.
     *
     * @return The application start time.
     */
    @Bean
    public Instant startTime() {
        return Instant.now();
    }

    /**
     * Allows the user to test their connectivity to foreman server and sends
     * confirmation of success or notifies of failure
     *
     * @param stateRepository the repository where {@link State}s are stored.
     * @param foremanApiUrl   the URL for the user foreman API
     *
     * @return returns the test command
     */
    @Bean
    public SlashCommandHandler testHandler(
            final StateRepository stateRepository,
            @Value("${foreman.apiUrl}") final String foremanApiUrl) {
        return new TestCommandHandler(stateRepository, foremanApiUrl);
    }
}
