# player-sdk-java
Player wrapper SDK written in Java

## Important Note

For reasons of the experimental status of the Ybrid® "v2" protocol it has been disabled by default.

## Maven Integration

In order to use 'player-sdk-java' in your project, you need to add the following 'repository' to the 'repositories' section of your project's pom:
```xml
    ...
    <repositories>
        ...
        <repository>
            <id>addradio-public-mvn-repo</id>
            <name>AddRadio Public Maven Repository</name>
            <url>http://mvn-repo.dev.addradio.net/mvn-repo/releases</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
        ...
    </repositories>
    ...
```
Then you also need to add the following dependency:
```xml
        <dependency>
            <groupId>io.ybrid</groupId>
            <artifactId>player-sdk-java</artifactId>
            <version>1.2.1</version>
        </dependency>
```

## Getting started
This package implements a basic player. It is based on Sessions handled by the API SDK.

Once a session was created a player can be created using a decoder factory and an audio backend factory.
The player implements the full protocol and can then be controlled using its API.

```java
import io.ybrid.api.MediaEndpoint;
import io.ybrid.api.session.Session;
import io.ybrid.api.session.Command;
import io.ybrid.player.player.Player;
import io.ybrid.player.player.YbridPlayer;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

class myPlayer {
    private void run() throws IOException {
        /* First create an Alias object and a Session. */
        final MediaEndpoint mediaEndpoint = new MediaEndpoint(new URI("https://stagecast.ybrid.io/adaptive-demo"));
        final Session session = mediaEndpoint.createSession();

        /* Connect the session to the server. */
        session.connect();

        /* We create a player using the Decoder and AudioBackend we provide */
        Player player = new YbridPlayer(session);

        /* We set the sink for the Metadata. This could be the user interface. */
        player.setMetadataConsumer(myMetadataConsumer);

        /* Before we can start playback we must signal the player to prepare. */
        player.prepare();

        /* After the player is prepared we can start playback.
         * If the player is not yet ready it will delay the playback until it is ready.
         */
        player.play();

        /* Now we can care of our own business */

        /* When we finish we stop the player. */
        player.stop();

        /* Closing the player is required and will free all resources
         * After close() returned the player must not be reused.
         */
        player.close();

        /* After the player finished close the session. */
        session.close();
    }
}
```

## Copyright
Copyright (c) 2019-2021 nacamar GmbH, Germany. See [MIT License](LICENSE) for details.
