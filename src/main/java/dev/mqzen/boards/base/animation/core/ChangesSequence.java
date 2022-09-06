package dev.mqzen.boards.base.animation;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
