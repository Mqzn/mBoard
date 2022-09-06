package dev.mqzen.boards.animation;

import dev.mqzen.boards.animation.core.Animation;
import dev.mqzen.boards.animation.core.ChangesSequence;
import org.checkerframework.checker.nullness.qual.NonNull;

@Deprecated
public final class ScrollAnimation extends Animation<String> {
	private final @NonNull Scroller scroller;

	public ScrollAnimation(String original, @NonNull Scroller scroller) {
		super(original, ChangesSequence.of());
		this.scroller = scroller;

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
