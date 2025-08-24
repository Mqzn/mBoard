package studio.mevera.scofi.entity;

import studio.mevera.scofi.animation.HighlightingAnimation;
import studio.mevera.scofi.animation.ScrollAnimation;
import studio.mevera.scofi.animation.core.Animation;
import studio.mevera.scofi.base.BoardAdapter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public interface Title<T> {
	
	/**
	 *
	 * A simple method to provide the text of the title
	 * that will be viewed to a certain player
	 * If animation is present, returns the next animation frame
	 * Otherwise returns the static content
	 *
	 * @return the text/content of this title
	 */
	Optional<T> get();
	
	/**
	 * Sets a {@link Animation} for the title
	 * @param animation the animation
	 * @return the title animation
	 * @param <TITLE> the type of title instance to return
	 */
	<TITLE extends Title<T>> TITLE withAnimation(@Nullable Animation<T> animation);
	
	/**
	 * A method to provide the animation of the title
	 * By default, a title has no animation unless
	 * you set its animation by overriding this method
	 * and returning your animation object
	 * e.g: Optional.ofNullable(yourAnimationInstance)
	 *
	 * @return the Animation of the title if present
	 */
	default Optional<Animation<T>> loadAnimation() {
		return Optional.empty();
	}
	
	static TitleImplementation.LegacyTitle legacy() {
		return new TitleImplementation.LegacyTitle();
	}
	static TitleImplementation.AdventureTitle adventure() {
		return new TitleImplementation.AdventureTitle();
	}
	
	default boolean hasAnimation() {
		return loadAnimation().isPresent();
	}
	
	class TitleImplementation<T> implements Title<T>{
		protected T content;
		private Animation<T> titleAnimation;
		public TitleImplementation() {
		
		}
		public TitleImplementation(T content) {
			this.content = content;
		}
		
		@Override
		public @NotNull Optional<T> get() {
			// FIXED: Check for animation first, similar to Line.fetchContent()
			if (titleAnimation != null) {
				return Optional.of(titleAnimation.fetchNextChange());
			}
			return Optional.of(content);
		}
		
		public void setContent(T content) {
			if(titleAnimation != null) {
				throw new IllegalStateException("You cannot set a title content while being based on animation");
			}
			this.content = content;
		}
		
		public void setTitleAnimation(Animation<T> titleAnimation) {
			this.titleAnimation = titleAnimation;
			if(titleAnimation != null) {
				this.content = titleAnimation.getOriginal();
			}
		}
		
		@Override @SuppressWarnings("unchecked")
		public <TITLE extends Title<T>> TITLE withAnimation(@Nullable Animation<T> animation) {
			setTitleAnimation(animation);
			return (TITLE) this;
		}
		
		/**
		 * A method to provide the animation of the title
		 * By default, a title has no animation unless
		 * you set its animation by overriding this method
		 * and returning your animation object
		 * e.g: Optional.ofNullable(yourAnimationInstance)
		 *
		 * @return the Animation of the title if present
		 */
		@Override
		public Optional<Animation<T>> loadAnimation() {
			return Optional.ofNullable(titleAnimation);
		}
		
		public static class LegacyTitle extends TitleImplementation<String> {
			
			public LegacyTitle ofText(String content) {
				super.setContent(ChatColor.translateAlternateColorCodes('&', content));
				return this;
			}
			
			public LegacyTitle withScroll(int width, int spaceBetween) {
				if(this.content == null) {
					throw new IllegalArgumentException("You cannot call withScroll() without calling #ofText() before it to set the text, " +
							"the scrolling animation will be based on no text," +
							" Alternatively you can set it using #withAnimation");
				}
				return super.withAnimation(ScrollAnimation.of(this.content, width, spaceBetween));
			}
			
			public LegacyTitle withHighlight(org.bukkit.ChatColor primaryColor, org.bukkit.ChatColor secondaryColor) {
				if(this.content == null) {
					throw new IllegalArgumentException("You cannot call withHighlight() without calling #ofText() before it to set the text, " +
							"the scrolling animation will be based on no text," +
							" Alternatively you can set it using #withAnimation");
				}
				return super.withAnimation(HighlightingAnimation.of(this.content, primaryColor, secondaryColor));
			}
		}
		public static class AdventureTitle extends TitleImplementation<Component> {
			
			public AdventureTitle ofComponent(Component content) {
				super.setContent(content);
				return this;
			}
		}
	}
	
}