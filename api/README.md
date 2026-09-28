# ServerChangelogs API

The ServerChangelogs API lets Paper and Velocity plugins publish and inspect changelogs, track whether players have
read them, open the changelog menu, and observe publish/read events. It is platform-neutral and does not expose the
plugin's internal storage or dialog implementation.

## Dependency and runtime setup

The API is published separately from the platform plugin:

```kotlin
dependencies {
    compileOnly("com.codingcat:serverchangelogs-api:<version>")
}
```

Use `compileOnly`; do not shade or relocate the API into your plugin. Your Paper or Velocity plugin must declare
ServerChangelogs as a required runtime dependency so it loads first and both plugins use the API classes provided by
ServerChangelogs.

The API module is configured to publish sources and Javadocs alongside the main Maven artifact. This repository does
not configure a remote Maven repository; release infrastructure must provide the repository URL and credentials.

## Getting the API

```java
if (!ServerChangelogsApi.isAvailable()) {
    throw new IllegalStateException("ServerChangelogs has not enabled");
}

ServerChangelogsApi api = ServerChangelogsApi.get();
```

| Method | What it does |
| --- | --- |
| `ServerChangelogsApi.isAvailable()` | Returns `true` after ServerChangelogs has enabled and registered its provider. It returns `false` before enable and after disable. |
| `ServerChangelogsApi.get()` | Returns the active API instance. It throws `IllegalStateException` when the plugin is unavailable. |

Keep the returned instance only while ServerChangelogs is enabled. A cached instance rejects calls with
`IllegalStateException` after ServerChangelogs disables. API calls are synchronous and the backing storage is not
thread-safe, so call all methods—including queries—from the server's main thread unless the active platform explicitly
guarantees another context.

## Changelog data

`Changelog` is an immutable snapshot with these accessors:

| Accessor | Value |
| --- | --- |
| `id()` | The changelog's current non-negative storage identifier. |
| `lines()` | An immutable `List<Component>` containing the displayed changelog lines. |
| `publishedAt()` | The `Instant` at which the changelog was created. |
| `author()` | An `Optional<Component>` containing the displayed author. |

The public `Changelog` constructor creates an immutable snapshot and rejects a negative ID, an empty line list, null
lines, a null timestamp, or a null `Optional`. Changelog IDs are current storage positions and may change when an
earlier entry is deleted, so integrations must not treat them as permanent external identifiers.

## Creating changelogs

```java
Changelog withoutAuthor = api.createChangelog(
        List.of(Component.text("Added a new game mode"))
);

Changelog withAuthor = api.createChangelog(
        List.of(
                Component.text("Added a new game mode"),
                Component.text("Updated the lobby")
        ),
        Component.text("Server Team")
);
```

| Method | What it does |
| --- | --- |
| `createChangelog(List<Component> lines)` | Persists a changelog without an author and returns its immutable snapshot. The line list must not be empty or contain null. |
| `createChangelog(List<Component> lines, Component author)` | Persists a changelog with an author and returns its immutable snapshot. Neither argument may be null. |

Both overloads fire one `ChangelogPublishedEvent` synchronously after storage succeeds.

## Reading changelogs

| Method | What it does |
| --- | --- |
| `getChangelogs()` | Returns an immutable list of immutable snapshots in publication order, oldest first. An empty list means no changelogs exist. |
| `getChangelog(int id)` | Returns the changelog with the current ID, or `Optional.empty()` when it does not exist. |
| `getLatestChangelog()` | Returns the most recently published changelog, or `Optional.empty()` when storage is empty. |

```java
api.getLatestChangelog().ifPresent(changelog ->
        logger.info("Latest changelog ID: {}", changelog.id())
);
```

## Player read state

Read state uses player UUIDs, so it also works when a player is offline.

| Method | What it does |
| --- | --- |
| `hasRead(int changelogId, UUID playerId)` | Returns `true` if that player has read the changelog. It returns `false` for an unknown changelog or an unread player. |
| `markAsRead(int changelogId, UUID playerId)` | Changes an existing changelog from unread to read for that player. It returns `true` only for the first successful transition and `false` if the changelog is missing or was already read. |

A successful transition fires one `ChangelogReadEvent`. Repeating `markAsRead` does not fire another event.

## Opening the menu

```java
boolean opened = api.openChangelogMenu(playerUuid);
```

`openChangelogMenu(UUID playerId)` opens the normal ServerChangelogs dialog for an online player and returns `true`.
It returns `false` when that UUID is not currently online. This method must be called from a context in which the
platform allows dialogs to be opened, normally the server's main thread.

## Events and subscriptions

Subscribe by exact event class:

```java
ChangelogSubscription publishedSubscription = api.subscribe(
        ChangelogPublishedEvent.class,
        event -> logger.info("Published changelog {}", event.changelog().id())
);

ChangelogSubscription readSubscription = api.subscribe(
        ChangelogReadEvent.class,
        event -> logger.info(
                "Player {} read changelog {}",
                event.playerId(),
                event.changelog().id()
        )
);
```

| API | What it does |
| --- | --- |
| `subscribe(Class<T> eventType, Consumer<? super T> listener)` | Registers a listener for that exact event type and returns its subscription handle. The event type and listener must not be null. |
| `ChangelogSubscription.unsubscribe()` | Removes the listener. Calling it repeatedly has no additional effect. |
| `ChangelogSubscription.close()` | Equivalent to `unsubscribe()`, allowing subscriptions to be managed as `AutoCloseable`. |
| `ChangelogPublishedEvent.changelog()` | Returns the immutable snapshot that was successfully published. |
| `ChangelogReadEvent.changelog()` | Returns the immutable snapshot that changed to read. |
| `ChangelogReadEvent.playerId()` | Returns the UUID of the player whose state changed. |

Listeners execute synchronously on the thread that caused the event. A listener exception is logged and isolated so it
does not prevent the remaining listeners from running. Keep every subscription and close it when your plugin disables:

```java
@Override
public void onDisable() {
    publishedSubscription.close();
    readSubscription.close();
}
```

ServerChangelogs also clears all remaining subscriptions when it shuts down.

## Provider-only members

`ServerChangelogsApi` has a protected constructor plus protected `registerProvider()` and `unregisterProvider()`
methods. These exist only for the implementation in the `base` module. Consumer plugins must not subclass
`ServerChangelogsApi` or attempt to register a provider.

## Complete example

```java
public final class ExampleIntegration {
    private final ServerChangelogsApi api = ServerChangelogsApi.get();
    private final ChangelogSubscription readSubscription;

    public ExampleIntegration() {
        this.readSubscription = api.subscribe(ChangelogReadEvent.class, event ->
                System.out.println(event.playerId() + " read #" + event.changelog().id())
        );
    }

    public void publishAndShow(UUID playerId) {
        Changelog changelog = api.createChangelog(
                List.of(Component.text("Maintenance completed")),
                Component.text("Operations")
        );

        if (!api.hasRead(changelog.id(), playerId)) {
            api.openChangelogMenu(playerId);
        }
    }

    public void close() {
        this.readSubscription.close();
    }
}
```
