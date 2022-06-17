package mn.foreman.slackbot.db.session;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * A {@link State} represents the bot's state for each registered chat
 * id.
 */
@Data
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class State {

    /** The API key. */
    private String apiKey;

    /** The chat id. */
    @Id
    private String chatId;

    /** The client ID. */
    private int clientId;

    /** When the session was added. */
    private Instant dateRegistered;

    /** The last notification id. */
    private int lastNotificationId;
}
