# mBoard
An Advanced ScoreBoard Library that was built and designed
by following critical OOP principles and modern design patterns.

This library's purpose is to make your life easier
by eliminating the much work that you have to put in to 
make a normal scoreboard using the old plain bukkit's api

## Get Started
[![](https://jitpack.io/v/Mqzn/mTime.svg)](https://jitpack.io/#Mqzn/mTime)

If you're using a dependency manager software like maven or gradle
then just follow the steps below to setup your project
and allow it to use the power of mTime !
otherwise, just [Download](https://github.com/Mqzn/mTime/releases/tag/1.1) the jar and add it as an artifact dependency


### Step 1: Add the repository

#### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

#### Gradle
```groovy

repositories {
    mavenCentral()
    maven {
        url 'https://jitpack.io'
    }
}
```

### Step 2: Add the dependency

#### Maven
```xml
<dependency>
    <groupId>com.github.Mqzn</groupId>
    <artifactId>mBoard</artifactId>
    <version>1.0</version>
</dependency>
```

###### Gradle
```groovy

dependencies {
    implementation 'com.github.mqzn:mBoard:1.0'
}
```

### Step 3: Shade the dependency

###### Maven
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.3.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>dev.mqzen.boards</pattern>
                        <!-- Replace 'com.yourpackage' with the package of your plugin ! -->
                        <shadedPattern>com.yourpackage.boards</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>
```

###### Gradle
Dont forget to add the shadow jar plugin in your build.gradle.
It should be above at the top of the build.gradle file.
```groovy 
plugins {
    id 'com.github.johnrengelman.shadow' version '7.1.2'
}
```
Now let's configure the shadow jar plugin
to relocate the mBoard dependency into your project.

```groovy
shadowJar {
    // Replace 'com.yourpackage' with the package of your plugin 
    relocate 'dev.mqzen.boards', 'com.yourpackage.boards'
}
```
## Usage
This is how you can stop wasting time on
creating scoreboards using bukkit's api

### Firstly, create an adapter

#### What's an adapter?
It's a class that returns some information that are needed
to be obtained in order to construct a scoreboard.
Such as e.g: Title or Lines

#### Creating your adapter
```java
public final class TestAdapter implements BoardAdapter {
	private final @NonNull Animation<String> titleAnimation = new Animation<>("&4&lmBoard",
					"&4&lmBoard",
					"&c&lm&4&lBoard",
					"&4&lm&c&lB&4&loard",
					"&4&lmB&c&lo&4&lard",
					"&4&lmBo&c&la&4&lrd",
					"&4&lmBoa&c&lr&4&ld",
					"&4&lmBoar&c&ld"
	);

	private final @NonNull Animation<String> yourWebsite = new Animation<>("      &eYourWebsite.com      ",
					"     &eYourWebsite.com ",
					"    &eYourWebsite.com",
					"   &eYourWebsite.com",
					" &eYourWebsite.com",
					"&eYourWebsite.com      ",
					"&eourWebsite.com      ",
					"&eurWebsite.com ",
					"&erWebsite.com  ",
					"&eWebsite.com   ",
					"&eebsite.com    ",
					"&ebsite.com     ",
					"&esite.com      ",
					"&eite.com       ",
					"&ete.com        ",
					"&ee.com         ",
					"&e.com          ",
					"&ecom           ",
					"&eom         ",
					"&em             ",
					"&r              ",
					"&r              ",
					"                   &eYo",
					"                 &eYour",
					"              &eYourWe ",
					"            &eYourWebs ",
					"          &eYourWebsit ",
					"        &eYourWebsite. ",
					"       &eYourWebsite.c ",
					"      &eYourWebsite.co ",
					"     &eYourWebsite.com ");


	@Override
	public @NonNull Title title(Player player) {
		return Title.builder()
						.withText("&6&lWelcome &e&l" + player.getName())
						.withAnimation(titleAnimation)
						.build();
	}

	@Override
	public @NonNull Body getBody(Player player) {
		Body body = Body.of("&7&l+------------------------+",
						"",
						"&8> &eThis is mBoard,say Hello",
						"");
		body.addNewLine(yourWebsite);
		body.addNewLine("&7&l+------------------------+");
		return body;
	}

	@Override
	public @NonNull BoardUpdate getBoardUpdate() {
		return (board) -> {
			board.updateTitle();
			board.updateBody();
		};
	}
}
```

#### Here's an example plugin class
```java
public class ExamplePlugin extends JavaPlugin implements Listener {

	private TestAdapter adapter;
	@Override
	public void onEnable() {

		adapter = new TestAdapter();
		Bukkit.getPluginManager().registerEvents(this, this);

		BoardManager.load(this);
		BoardManager.getInstance().setUpdateInterval(5L); //default is 2L
		BoardManager.getInstance().startBoardUpdaters();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		BoardManager.getInstance().setupNewBoard(e.getPlayer(), adapter);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		BoardManager.getInstance().removeBoard(e.getPlayer());
	}

}
```

### Credits
- To MrMicky for creating FastBoard, because originally this is a fork of it.
- To me of course for spending 3 days on this awesome library

