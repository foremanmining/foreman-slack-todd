package mn.foreman.slackbot.controllers;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

import javax.servlet.annotation.WebServlet;

/** Controller for the Apps Oauth installation */
@WebServlet("/slack/install")
public class SlackOAuthInstallController
    extends SlackOAuthAppServlet {

    /**
     * Constructor
     *
     * @param app The {@link App}.
     */
    public SlackOAuthInstallController(final App app) { super(app); }
}
