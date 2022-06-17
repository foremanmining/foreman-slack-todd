package mn.foreman.slackbot.config;

import mn.foreman.slackbot.db.session.StateRepository;
import mn.foreman.slackbot.handlers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.handler.builtin.SlashCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * The two parts of this app are the oauth to allow other users to use
 * this bot followed by the functionality for the slash commands on Slack
 * for now we have start, register, forget, test, and help
 *
 */
@Configuration
public class BotConfig {

    /** The logger for this class. */
    private static final Logger LOG =
            LoggerFactory.getLogger(BotConfig.class);

    /** The signing secret for the user */
    private final String signingSecret;

    /** The users client Id */
    private final String clientId;

    /** Secret for the client */
    private final String clientSecret;

    /**
     * Constructor for the Bot
     *
     * @param signingSecret the users signing secret
     * @param clientId the person using the apps client Id
     * @param clientSecret the person using the apps client Id
     */
    public BotConfig(@Value("${credentials.signingSecret}") final String signingSecret,
                     @Value("${credentials.clientId}")final String clientId,
                     @Value("${credentials.clientSecret}")final String clientSecret) {
        this.signingSecret = signingSecret;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }


    @Bean
    public SlashCommandHandler forgetHandler(final StateRepository stateRepository) {
        return new ForgetCommandHandler(stateRepository);
    }

    @Bean
    public SlashCommandHandler helpHandler() {
        return new HelpCommandHandler();
    }

    @Bean
    public App initSlackApp(
            final SlashCommandHandler startHandler,
            final SlashCommandHandler registerHandler,
            final SlashCommandHandler forgetHandler,
            final SlashCommandHandler testHandler,
            final SlashCommandHandler helpHandler) {
        final App app = new App();

        final AppConfig appConfig =
                AppConfig
                        .builder()
                        .clientId(this.clientId)
                        .clientSecret(this.clientSecret)
                        .signingSecret(this.signingSecret)
                        .scope("chat:write,commands,chat:read,app_mentions:read," +
                                "chat:write.customize")
                        .oauthInstallPath("install")
                        .oauthRedirectUriPath("oauth_redirect")
                        .build();

        final App oauth = new App(appConfig);
        oauth.asOAuthApp(true);

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

    @Bean
    public SlashCommandHandler testHandler(
            final StateRepository stateRepository,
            @Value("${foreman.apiUrl}") final String foremanApiUrl) {
        return new TestCommandHandler(stateRepository, foremanApiUrl);
    }
}
