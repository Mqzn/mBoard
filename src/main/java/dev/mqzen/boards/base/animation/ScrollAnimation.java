package dev.mqzen.boards.base.animation;

public final class TextAnimation extends Animation<String> {

	public TextAnimation(String original, String... sequence) {
		super(original, sequence);
	}

	public TextAnimation(String original, Scroller scroller) {
		super(original, readSequence(scroller));

	}

	private static ChangesSequence<String> readSequence(Scroller scroller) {

		ChangesSequence<String> sequence = ChangesSequence.of();
		for (String next : scroller) {
			System.out.println("NEXT LENGTH: " + next.length());
			if(next.length() >= 2) {
				sequence.add(next);
			}
		}

		return sequence;
	}

}
