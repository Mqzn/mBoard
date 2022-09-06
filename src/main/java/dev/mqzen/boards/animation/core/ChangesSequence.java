package dev.mqzen.boards.animation.core;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 *
 * The class which is responsible for
 * caching the change phases
 * it's also iterable, so you can put it
 * in an enhanced for loop to
 *
 * e.g:
 * ChangeSequence<String> sequence = ChangeSequence.of("Hello", "Welcome !)
 * for(String change : sequence) {
 *  //your code here
 * }
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 *
 * @param <T> the type to be changed
 */
public class ChangesSequence<T> implements Iterable<T> {

	private final List<T> changes = new ArrayList<>();

	@SafeVarargs
	ChangesSequence(T... changes) {
		this.changes.addAll(Arrays.asList(changes));
	}

	@SafeVarargs
	public static <T> ChangesSequence<T> of(T... changes) {
		return new ChangesSequence<>(changes);
	}


	@NonNull
	@Override
	public Iterator<T> iterator() {
		return changes.iterator();
	}

	public @Nullable T getChange(int index) {
		if(index >= changes.size() || index < 0) {
			return null;
		}
		return changes.get(index);
	}

	public void add(T change) {
		changes.add(change);
	}

	int length() {
		return changes.size();
	}

}
