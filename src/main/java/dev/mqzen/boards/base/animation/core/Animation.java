package dev.mqzen.boards.base.animation;

import lombok.Getter;

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

}
