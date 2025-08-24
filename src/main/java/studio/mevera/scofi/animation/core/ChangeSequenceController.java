package studio.mevera.scofi.animation.core;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 *
 * The class that controls the sequence of changes
 * of each phase of the animation
 *
 * @see Animation<T>
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 *
 * @param <T> the type to be changed
 */

public class ChangeSequenceController<T> {

	private @Getter int changeIndex = 0;
	private final ChangesSequence<T> sequence;

	protected ChangeSequenceController(ChangesSequence<T> sequence) {
		this.sequence = sequence;
	}

	public static <T> ChangeSequenceController<T> newController(ChangesSequence<T> sequence){
		return new ChangeSequenceController<>(sequence);
	}


	public @NotNull T next()  {
		if(changeIndex >= sequence.length()) {
			changeIndex = 0;
		}

		T change = sequence.getChange(changeIndex);
		assert change != null;

		changeIndex++;
		return change;
	}

	public @NotNull T previous() {
		changeIndex--;

		if(changeIndex < 0) {
			changeIndex = sequence.length()-1;
		}

		T change = sequence.getChange(changeIndex);
		assert change != null;
		return change;
	}


}
