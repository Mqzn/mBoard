package studio.mevera.scofi.animation.core;

import lombok.Getter;

/**
 * A simple class that caches the sequence of an animation
 * and the original value of the sequence
 * in order to switch to the next phase of the animation
 * using the class {@link ChangeSequenceController<T>}
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 * @param <T> the object type that's going be changed through
 *           a sequence of changes
 */
public class Animation<T>  {
	protected final @Getter T original;
	private final ChangeSequenceController<T> controller;

	public Animation(T original, ChangesSequence<T> sequence) {
		this.original = original;
		this.controller = ChangeSequenceController.newController(sequence);
	}
	@SafeVarargs
	public Animation(T original, T... sequence) {
		this.original = original;
		this.controller = ChangeSequenceController.newController(ChangesSequence.of(sequence));
	}

	public T fetchNextChange() {
		return controller.next();
	}

	public T fetchPreviousChange() {
		return controller.previous();
	}

	public int current() {
		return controller.getChangeIndex();
	}

}
