package mn.foreman.slackbot.controllers;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;

import javax.servlet.annotation.WebServlet;

/** A controller to serve the {@link App}. */
@WebServlet("/slack/events")
public class SlackAppController
        extends SlackAppServlet {

    /**
     * Constructor.
     *
     * @param app The {@link App}.
     */
    public SlackAppController(final App app) {
        super(app);
    }
}
