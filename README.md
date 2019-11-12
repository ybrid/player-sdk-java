# player-sdk-java
Player wrapper SDK written in Java

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
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
```

## Getting started
... TODO ...

```java
/* ... TODO ... */
```

## Copyright
Copyright (c) 2019 nacamar GmbH, Germany. See [Apache License 2.0](LICENSE) for details.
