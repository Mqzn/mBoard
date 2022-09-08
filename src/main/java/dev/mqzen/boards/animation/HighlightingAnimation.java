package dev.mqzen.boards.animation;

import dev.mqzen.boards.animation.core.Animation;
import org.bukkit.ChatColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class HighlightingAnimation extends Animation<String> {

	private final @NonNull HighLighter highLighter;
	private int position=0;

	private HighlightingAnimation(@NonNull String message,
	                              @NonNull String primaryColor,
	                              @NonNull String secondaryColor) {
		super(message);
		this.highLighter = HighLighter.of(message, primaryColor, secondaryColor);
	}

	private HighlightingAnimation(@NonNull String message,
	                              @NonNull ChatColor primaryColor,
	                              @NonNull ChatColor secondaryColor) {
		super(message);
		this.highLighter = HighLighter.of(message, primaryColor, secondaryColor);
	}

	public static HighlightingAnimation of(@NonNull String message,
	                                       @NonNull ChatColor primaryColor,
	                                       @NonNull ChatColor secondaryColor) {
		return new HighlightingAnimation(message, primaryColor, secondaryColor);
	}

	public static HighlightingAnimation of(@NonNull String message,
	                                       @NonNull String primaryColor,
	                                       @NonNull String secondaryColor) {
		return new HighlightingAnimation(message, primaryColor, secondaryColor);
	}

	@Override
	public String fetchNextChange() {
		return this.highLighter.nextResult();
	}

	@Override
	public String fetchPreviousChange() {
		if(position < 0) {
			position = 0;
		}
		String prev = highLighter.getHighLighted(position);
		position--;
		return prev;
	}

}
