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
	
	@Override
	public @NonNull Title title(Player player) {
		return Title.builder().withText("&4&lmBoard")
				      .withAnimation(titleAnimation)
				      .build();
	}

	@Override
	public @NonNull Body getBody(Player player) {
		return Body.of(
						"&7&l+------------------------+",
						"",
						"&8> &eThis is mBoard,say Hello",
						"", 
                        "&7&l+------------------------+");
	}

}
```

### Animations
Animated Lines are basically dynamic lines that change every x tick
where x is the **update interval**

#### Well known animations
There are several types of animations and there are some common animations
that requires advanced processing to the lines contents such as Highlighting a line
or even making a scrolling line

##### Example
```java
public class TestAdapter implements BoardAdapter {
	/**
	 * Fetches the title to be represented
	 * on the board that has this adapter instance;
	 *
	 * @param player the player who will view the title
	 * @return the title of the scoreboard
	 */
	@Override
	public @NonNull Title title(Player player) {
		return Title.builder()
						.withText("Hello")
						.withAnimation(ScrollAnimation.of("&eHello", 32/*width of the scrolling*/, 1/*the distance that's moved*/))
						.build();
	}

	/**
	 * Gets the body to be represented
	 * as the body of the scoreboard
	 * which will occupy this adapter as it's
	 * model or template to take data from.
	 *
	 * @param player the player who will view the lines
	 * @return the body of the scoreboard
	 */
	@Override
	public @NonNull Body getBody(Player player) {
		Body body = Body.of("&7&l&m+----------------+");
		body.addNewLine(HighlightingAnimation.of("Test Hello", ChatColor.GOLD, ChatColor.YELLOW));
		body.addNewLine("&7&l&m+-----------------+");
		return body;
	}

}
```

###### **Result**
![](https://github.com/Mqzn/mBoard/blob/master/src/main/results/mBoard-gif-2.gif)

#### Manual Animations
Sometimes there's a situation where you want to implement your own type of change phases
to create your own animation the way you just want so, you will have
to do it manually by specifying each change phase in the form of a string

##### Example
```java
public class TestAdapter implements BoardAdapter {

	private final Animation<String> titleAnimation = new Animation<>("&4mBoard &7| &cA lib" /*message*/,
					"&4mBoard ",
					"&4mBoard &fT",
					"&4mBoard &fTh",
					"&4mBoard &fThe",
					"&4mBoard &fThe &7#",
					"&4mBoard &fThe &7#1 ",
					"&4mBoard &fThe &7#1 &cL",
					"&4mBoard &fThe &7#1 &cLi",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLib",
					"&4mBoard &fThe &7#1 &cLi",
					"&4mBoard &fThe &7#1 &cL",
					"&4mBoard &fThe &7#1 ",
					"&4mBoard &fThe &7#",
					"&4mBoard &fThe",
					"&4mBoard &fTh",
					"&4mBoard &fT",
					"&4mBoard ",
					"&4mBoard ");


	/**
	 * Fetches the title to be represented
	 * on the board that has this adapter instance;
	 *
	 * @param player the player who will view the title
	 * @return the title of the scoreboard
	 */

	@Override
	public @NonNull Title title(Player player) {
		return Title.builder()
						.withText("&4mBoard &7| &cA lib")
						.withAnimation(titleAnimation)
						.build();
	}

	/**
	 * Gets the body to be represented
	 * as the body of the scoreboard
	 * which will occupy this adapter as it's
	 * model or template to take data from.
	 *
	 * @param player the player who will view the lines
	 * @return the body of the scoreboard
	 */
	@Override
	public @NonNull Body getBody(Player player) {
		Body body = Body.of("&7&l&m+----------------+");
		body.addNewLine(HighlightingAnimation.of("Test Highlighted", ChatColor.GOLD, ChatColor.YELLOW));
		body.addNewLine("&7&l&m+-----------------+");
		return body;
	}

}

```

###### **Result**
![](https://github.com/Mqzn/mBoard/blob/master/src/main/results/mBoard-gif-3.gif)


#### Here's an example plugin class
```java
public class ExamplePlugin extends JavaPlugin implements Listener {

	private TestAdapter adapter;
	@Override
	public void onEnable() {

		adapter = new TestAdapter();
		Bukkit.getPluginManager().registerEvents(this, this);

		BoardManager.load(this);
		BoardManager.getInstance().setUpdateInterval(4L); //default is 2L
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

### Results
![](https://github.com/Mqzn/mBoard/blob/master/src/main/results/mBoard-animation-fixed.gif)

### Credits
- To MrMicky for creating FastBoard, because originally this is a fork of it.
- To me of course for spending 3 days on this awesome library

