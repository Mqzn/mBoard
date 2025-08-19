package dev.mqzen.boards.animation;

import dev.mqzen.boards.animation.core.Animation;
import dev.mqzen.boards.animation.core.ChangesSequence;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public final class ScrollAnimation extends Animation<String> {
	private final @NotNull Scroller scroller;

	private ScrollAnimation(String original, int width, int spaceBetween) {
		super(original, ChangesSequence.of());
		this.scroller = Scroller.of(ChatColor.translateAlternateColorCodes('&', original), width, spaceBetween);
	}

	public static ScrollAnimation of(String msg, int width, int spaceBetween) {
		return new ScrollAnimation(msg, width, spaceBetween);
	}

	@Override
	public String fetchNextChange() {
		return scroller.next();
	}

	@Override
	public String fetchPreviousChange() {
		return scroller.next();
	}

}
