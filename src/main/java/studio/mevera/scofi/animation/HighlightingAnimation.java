package studio.mevera.scofi.animation;

import studio.mevera.scofi.animation.core.Animation;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class HighlightingAnimation extends Animation<String> {

	private final @NotNull HighLighter highLighter;
	private int position=0;

	private HighlightingAnimation(@NotNull String message,
	                              @NotNull String primaryColor,
	                              @NotNull String secondaryColor) {
		super(message);
		this.highLighter = HighLighter.of(message, primaryColor, secondaryColor);
	}

	private HighlightingAnimation(@NotNull String message,
	                              @NotNull ChatColor primaryColor,
	                              @NotNull ChatColor secondaryColor) {
		super(message);
		this.highLighter = HighLighter.of(message, primaryColor, secondaryColor);
	}

	public static HighlightingAnimation of(@NotNull String message,
	                                       @NotNull ChatColor primaryColor,
	                                       @NotNull ChatColor secondaryColor) {
		return new HighlightingAnimation(message, primaryColor, secondaryColor);
	}

	public static HighlightingAnimation of(@NotNull String message,
	                                       @NotNull String primaryColor,
	                                       @NotNull String secondaryColor) {
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
