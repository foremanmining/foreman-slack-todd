package mn.foreman.slackbot;

import mn.foreman.slackbot.db.session.StateRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Foreman Telegram bot.
 */
@SpringBootApplication
@EnableMongoRepositories(basePackageClasses = StateRepository.class)
@EnableScheduling
@ServletComponentScan
public class SlackBotApplication {

    /**
     * Application entry point.
     *
     * @param args The command line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(
                SlackBotApplication.class,
                args);
    }
}