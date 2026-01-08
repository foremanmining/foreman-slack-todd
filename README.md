# foreman-slack-todd

The Foreman Slack bot - sends mining notifications from your Foreman dashboard directly to Slack channels.

## Overview

This is a Spring Boot application that integrates with Slack's API to deliver real-time notifications about your mining operations. Users can register their Foreman API credentials with the bot, and it will periodically check for new notifications and post them to the registered Slack channel.

## Tech Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Spring Boot 2.7.0 |
| **Language** | Java 11 |
| **Database** | MongoDB |
| **Slack SDK** | Slack Bolt SDK 1.22.1 |
| **Build Tool** | Maven |
| **Container** | Docker (via Jib) |

## Project Structure

```
src/main/java/mn/foreman/slackbot/
├── SlackBotApplication.java      # Main entry point
├── config/
│   ├── BotConfig.java            # Spring beans & Slack app configuration
│   └── ForemanUtils.java         # Helper to create Foreman API clients
├── controllers/
│   ├── SlackAppController.java   # Handles Slack events
│   ├── SlackOAuthInstallController.java
│   └── SlackOauthRedirectController.java
├── db/session/
│   ├── State.java                # MongoDB entity for user sessions
│   └── StateRepository.java      # MongoDB repository interface
├── handlers/                     # Slash command handlers
│   ├── StartCommandHandler.java
│   ├── RegisterCommandHandler.java
│   ├── ForgetCommandHandler.java
│   ├── TestCommandHandler.java
│   └── HelpCommandHandler.java
└── notifications/
    ├── Notifier.java             # Scheduled job that checks for notifications
    ├── NotificationsProcessor.java
    └── NotificationsProcessorImpl.java
```

## Slash Commands

| Command | Description |
|---------|-------------|
| `/foreman_start` | Introduction and setup instructions |
| `/foreman_register <clientId> <apiKey>` | Register your Foreman credentials to receive notifications |
| `/foreman_forget` | Unregister and stop receiving notifications |
| `/foreman_test` | Test connectivity to Foreman servers |
| `/foreman_help` | Show available commands |

## Database Schema

### MongoDB Collection: `state`

| Field | Type | Description |
|-------|------|-------------|
| `chatId` | String (ID) | Slack channel ID (primary key) |
| `clientId` | int | Foreman client ID |
| `apiKey` | String | Foreman API key |
| `botToken` | String | Slack bot OAuth token for this workspace |
| `dateRegistered` | Instant | When the channel was registered |
| `lastNotificationId` | int | ID of last sent notification (prevents duplicates) |

## Configuration

Configuration is in `src/main/resources/application.yaml`:

```yaml
bot:
  check:
    initialDelay: 60000    # Wait 60s before first notification check
    fixedDelay: 60000      # Check for notifications every 60s
  scope: chat:write,commands
  oauth:
    installPath: /slack/install
    redirectUriPath: /slack/oauth_redirect
    completionUrl: "OAUTH_COMPLETION_URL"
    cancellationUrl: "OAUTH_CANCELLATION_URL"
  credentials:
    botToken: "BOT_TOKEN"
    signingSecret: "SIGNING_SECRET"
    clientId: "CLIENT_ID"
    clientSecret: "CLIENT_SECRET"
  rootDir: my_path_here    # Directory for OAuth state files

notifications:
  max: 10                  # Max miners shown per notification

foreman:
  baseUrl: https://dashboard.foreman.mn
  apiUrl: https://api.foreman.mn

server:
  port: 3000

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/slackbot  # Set via env var in production
```

## How It Works

1. **OAuth Installation**: Slack workspace admins install the bot via OAuth flow
2. **Registration**: Users run `/foreman_register <clientId> <apiKey>` in a channel
3. **Credential Validation**: Bot pings Foreman API to verify credentials
4. **Session Storage**: Valid sessions are saved to MongoDB with the channel ID as key
5. **Notification Polling**: Every 60 seconds, the `Notifier` scheduled job:
   - Fetches all registered sessions from MongoDB
   - Queries Foreman API for new notifications (since last check)
   - Posts formatted messages to each registered Slack channel
   - Updates `lastNotificationId` to prevent duplicate notifications

## Dependencies

Key dependencies from `pom.xml`:

- `com.slack.api:bolt` - Slack Bolt SDK for slash commands and events
- `com.slack.api:bolt-servlet` - Servlet adapter for Slack
- `mn.foreman:java-api` - Foreman's Java API client (internal library)
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-web` - Web server for OAuth endpoints
- `lombok` - Reduces boilerplate code

## Building & Running

### Local Development

```bash
# Requires MongoDB running locally
mvn spring-boot:run
```

### Build Docker Image

```bash
mvn package jib:dockerBuild
```

### Deploy

The app is configured to push to Docker Hub (`delawr0190/slack-todd`) on `mvn deploy`.

## Environment Variables

In production, override these via environment variables:

| Variable | Description |
|----------|-------------|
| `BOT_CREDENTIALS_SIGNINGSECRRET` | Slack signing secret |
| `BOT_CREDENTIALS_CLIENTID` | Slack app client ID |
| `BOT_CREDENTIALS_CLIENTSECRET` | Slack app client secret |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection string |

## License

See [LICENSE](LICENSE) file.
