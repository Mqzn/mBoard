package dev.mqzen.boards.base.body.lines;

import dev.mqzen.boards.base.animation.core.Animation;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class represents a line in the scoreboard.
 * it caches  the index of the line, and it's original content
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 */
public final class Line {

	public static int MAX_LINES = 15;

	private final @NonNull @Getter String content;
	private final @Getter int index;

	private @Nullable @Getter Animation<String> animation;
	private Line(@NonNull String content, int index) {
		if(index > MAX_LINES) {
			throw new IllegalArgumentException("line's index is greater than 15");
		}
		this.content = ChatColor.translateAlternateColorCodes('&', content);
		this.index = index;
	}


	public Line setAnimation(@Nullable Animation<String> animation) {
		this.animation = animation;
		return this;
	}

	/**
	 * Creates a new instance of a line object
	 * to be placed in the body of your scoreboard
	 *
	 * @param content the content of the line
	 * @param index the index of the line
	 *
	 * @return The NEW line instance;
	 */
	public static Line of(
	                      @NonNull String content,
	                      int index) {
		return new Line(content, index);
	}

	/**
	 * Creates a new line instance
	 * but with empty content
	 *
	 * @param index the index of the empty line
	 * @return A NEW instance of the line with the empty content
	 */
	public static Line empty(int index) {
		return new Line("", index);
	}

	/**
	 * Simple method to fetch the length of a line's content
	 * @return the length of the content of the line
	 */
	public int length() {
		return content.length();
	}

	/**
	 * Simple method to determine whether
	 * this line has animation or no
	 *
	 * @return whether this line has animation or no
	 */
	public boolean isAnimated() {
		return animation != null;
	}


}
