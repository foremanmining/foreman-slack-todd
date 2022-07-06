package mn.foreman.slackbot.controllers;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

import javax.servlet.annotation.WebServlet;

/** Controller for oath redirect */
@WebServlet("/slack/oauth_redirect")
public class SlackOauthRedirectController
    extends SlackOAuthAppServlet{
    public SlackOauthRedirectController(final App app) { super(app); }
}
