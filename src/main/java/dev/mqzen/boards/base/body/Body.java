package dev.mqzen.boards.base.body;

import dev.mqzen.boards.base.Title;
import dev.mqzen.boards.base.animation.core.Animation;
import dev.mqzen.boards.base.body.lines.Line;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Does the same as it's name...
 * This class refers to the "Body" of the scoreboard
 * which consists of a title and
 * a list of ordered lines
 *
 * You can also iterate over the body of a scoreboard
 * e.g: for(Line line : scoreboardBody) {
 *        // you code here
 *      }
 *
 *
 * @see Title
 * @see Line
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 *
 */
public class Body implements Iterable<Line> {

	private final @Getter List<Line> lines;

	private Body(Body otherBody) {
		this.lines = new CopyOnWriteArrayList<>(otherBody.getLines());
	}
	private Body() {
		this.lines = new CopyOnWriteArrayList<>();
	}

	public static Body of(Body otherBody) {
		return new Body(otherBody);
	}

	public static Body of(String... lines) {

		Body body = empty();
		for (int i = 0; i < lines.length; i++) {
			String content = lines[i];
			body.addLine(Line.of(content, i));
		}

		return body;
	}

	public static Body empty() {
		return new Body();
	}

	public @Nullable Line getLine(int index) {
		if(index >= size() || index < 0) return null;
		return lines.get(index);
	}

	public void addLine(Line line) {
		if(lines.size() >= 15)
			return;

		lines.add(line.getIndex(), line);
	}

	public void removeLine(int index) {
		lines.remove(index);
	}

	public void updateLine(int index, Line line) {
		lines.set(index, line);
	}
	public void updateLineContent(int index, String newContent) {
		lines.set(index, Line.of(newContent, index));
	}

	public void addNewLine(String content) {
		int lastIndex = lines.size();//-1
		lines.add(Line.of(content, lastIndex/* +1 */));
	}
	public void addNewLine(Animation<String> animation) {
		int lastIndex = lines.size();//-1
		lines.add(Line.of(animation.getOriginal(), lastIndex/* +1 */)
						.setAnimation(animation));
	}


	public int size() {
		return lines.size();
	}

	@NonNull
	@Override
	public Iterator<Line> iterator() {
		return lines.iterator();
	}

	public void addAll(Line... lines) {
		this.lines.addAll(Arrays.asList(lines));
	}
	public void addAll(Body body) {
		this.lines.addAll(body.getLines());
	}

	public void clearAll() {
		lines.clear();
	}

	public final void debug() {
		System.out.println("----> Body Start <----");
		for(Line line : lines) {
			System.out.println("LINE #" + line.getIndex() + ": " + line.getContent());
		}
		System.out.println("----> Body End <----");
	}

}
