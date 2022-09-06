package dev.mqzen.boards.base.animation;


import org.checkerframework.checker.nullness.qual.NonNull;

public class ChangeSequenceController<T> {

	private int changeIndex = 0;
	private final ChangesSequence<T> sequence;

	protected ChangeSequenceController(ChangesSequence<T> sequence) {
		this.sequence = sequence;
	}

	public static <T> ChangeSequenceController<T> newController(ChangesSequence<T> sequence){
		return new ChangeSequenceController<>(sequence);
	}


	public @NonNull T next()  {
		if(changeIndex >= sequence.length()) {
			changeIndex = 0;
		}

		T change = sequence.getChange(changeIndex);
		assert change != null;

		changeIndex++;
		return change;
	}

	public @NonNull T previous() {
		changeIndex--;

		if(changeIndex < 0) {
			changeIndex = sequence.length()-1;
		}

		T change = sequence.getChange(changeIndex);
		assert change != null;
		return change;
	}


}
