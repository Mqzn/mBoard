package dev.mqzen.boards.base;

import dev.mqzen.boards.base.animation.core.Animation;
import lombok.Data;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 *
 * A simple interface to represent the data required
 * to be obtained about the title of a scoreboard
 *
 * @see BoardAdapter
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 *
 */
public interface Title {

	/**
	 *
	 * A simple method to provide the text of the title
	 * that will be viewed to a certain player
	 *
	 * @param player the viewer of the title
	 * @return the text/content of this title
	 */
	@NonNull Optional<String> asText(@NonNull Player player);

	/**
	 * A method to provide the length of the text of a title
	 *
	 * @return the length of the text of the title
	 */
	int length();

	/**
	 * A method to provide the animation of the title
	 * By default, a title has no animation unless
	 * you set its animation by overriding this method
	 * and returning your animation object
	 * e.g: Optional.ofNullable(yourAnimationInstance)
	 *
	 * @return the Animation of the title if present
	 */
	default Optional<Animation<String>> loadAnimation() {
		return Optional.empty();
	}

	default boolean hasAnimation() {
		return loadAnimation().isPresent();
	}

	/**
	 * Creates a builder that will work
	 * it's way through the methods to build up a
	 * proper instance of the Title object.
	 *
	 * @see Title.Builder
	 * @since 1.0
	 *
	 * @author Mqzen (aka Mqzn)
	 *
	 * @return the builder created.
	 */
	static Title.Builder builder() {
		return new Builder();
	}

	class Builder {

		private @Nullable Animation<String> titleAnimation;
		private @MonotonicNonNull String text;

		private Builder() {}


		public Builder withText(String text) {
			this.text = ChatColor.translateAlternateColorCodes('&', text);
			return this;
		}

		public Builder withAnimation(@Nullable Animation<String> titleAnimation) {
			this.titleAnimation = titleAnimation;
			return this;
		}

		public Title build() {
			TitleImpl impl = new TitleImpl(text);
			impl.setAnimation(this.titleAnimation);
			return impl;
		}

	}

	@Data
	class TitleImpl implements Title {

		private final String text;
		private @Setter Animation<String> animation;

		@Override
		public @NonNull Optional<String> asText(@NonNull Player player) {
			return Optional.of(text);
		}

		@Override
		public int length() {
			if(text == null) return 2;
			return text.length();
		}

		@Override
		public Optional<Animation<String>> loadAnimation() {
			return Optional.of(animation);
		}

	}

}
