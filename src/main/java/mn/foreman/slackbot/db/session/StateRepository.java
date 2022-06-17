package mn.foreman.slackbot.db.session;

import org.springframework.data.mongodb.repository.MongoRepository;

/** A repository for storing {@link State sessions}. */
public interface StateRepository
        extends MongoRepository<State, String> {
}
