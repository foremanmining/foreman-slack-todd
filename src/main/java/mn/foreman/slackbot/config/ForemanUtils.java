package mn.foreman.slackbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import mn.foreman.api.ForemanApi;
import mn.foreman.api.ForemanApiImpl;
import mn.foreman.api.JdkWebUtil;

import java.util.concurrent.TimeUnit;

/**
 * Utilities for interacting with the Foreman API.
 */
public class ForemanUtils {

    /**
     * Returns a new {@link ForemanApi} handler.
     *
     * @param clientId       The client ID.
     * @param apiKey         The client API key.
     * @param foremanBaseUrl The Foreman base URL.
     * @return The new API handler.
     */
    public static ForemanApi toApi(
            final int clientId,
            final String apiKey,
            final String foremanBaseUrl) {
        return new ForemanApiImpl(
                Integer.toString(clientId),
                "",
                new ObjectMapper(),
                new JdkWebUtil(
                        foremanBaseUrl,
                        apiKey,
                        5,
                        TimeUnit.SECONDS));
    }
}
