package studio.mevera.scofi.entity;

import studio.mevera.scofi.animation.HighlightingAnimation;
import studio.mevera.scofi.animation.ScrollAnimation;
import studio.mevera.scofi.animation.core.Animation;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

public interface Line<T> {
    T getContent();
    void setContent(T content);
    int getIndex();
    void setIndex(int index);
    Animation<T> getAnimation();
    void setAnimation(Animation<T> animation);
    
    default T fetchContent(){
        return getAnimation() == null ? getContent() : getAnimation().fetchNextChange();
    }
    
    // Builder factory methods
    static LegacyLineBuilder legacy(String content){
        return new LegacyLineBuilder(content);
    }
    
    static AdventureLineBuilder adventure(Component content){
        return new AdventureLineBuilder(content);
    }
    
    // Backwards compatibility
    static LineImplementation.LegacyLine legacy(String content, int index){
        return new LineImplementation.LegacyLine(content, index);
    }
    
    static LineImplementation.AdventureLine adventure(Component content, int index){
        return new LineImplementation.AdventureLine(content, index);
    }
    
    // Builder classes
    class LegacyLineBuilder {
        private final String content;
        private Animation<String> animation;
        
        public LegacyLineBuilder(String content) {
            this.content = ChatColor.translateAlternateColorCodes('&', content);
        }
        
        /**
         * Add a highlighting animation to the line's content
         */
        public LegacyLineBuilder withHighlight(String primaryColor, String secondaryColor) {
            this.animation = HighlightingAnimation.of(this.content, primaryColor, secondaryColor);
            return this;
        }
        
        /**
         * Add a highlighting animation to the line's content
         */
        public LegacyLineBuilder withHighlight(ChatColor primary, ChatColor secondary) {
            this.animation = HighlightingAnimation.of(this.content, primary, secondary);
            return this;
        }
        
        /**
         * Add a scroll animation to the line's content
         */
        public LegacyLineBuilder withScroll(int width, int spaceBetween) {
            this.animation = ScrollAnimation.of(this.content, width, spaceBetween);
            return this;
        }
        
        /**
         * Add a custom animation (must use the line's content as base)
         */
        public LegacyLineBuilder withAnimation(Animation<String> customAnimation) {
            // Validate that the animation is based on this line's content
            if (customAnimation != null && !this.content.equals(customAnimation.getOriginal())) {
                throw new IllegalStateException("Animation's content '" + customAnimation.getOriginal() + "' does not match the line's content '" + content + "'");
            }
            this.animation = customAnimation;
            return this;
        }
        
        /**
         * Build the Line instance
         */
        public LineImplementation.LegacyLine build() {
            LineImplementation.LegacyLine line = new LineImplementation.LegacyLine(content, -1);
            if (animation != null) {
                line.setAnimation(animation);
            }
            return line;
        }
    }
    
    class AdventureLineBuilder {
        private final Component content;
        private Animation<Component> animation;
        
        public AdventureLineBuilder(Component content) {
            this.content = content;
        }
        
        /**
         * Add a highlighting animation to the line's content
         * Note: For components, this converts to legacy format for animation
         */
        public AdventureLineBuilder withHighlight(ChatColor primary, ChatColor secondary) {
            // Convert component to legacy for highlighting animation
            // This is a limitation - proper component animation would need a component-aware highlighter
            String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(this.content);
            
            // Create animation and convert back
            Animation<String> legacyAnim = HighlightingAnimation.of(legacy, primary, secondary);
            
            this.animation = new Animation<Component>(this.content) {
                @Override
                public Component fetchNextChange() {
                    String animatedLegacy = legacyAnim.fetchNextChange();
                    return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(animatedLegacy);
                }
            };
            return this;
        }
        
        /**
         * Add a scroll animation to the line's content
         */
        public AdventureLineBuilder withScroll(int width, int spaceBetween) {
            String legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().serialize(this.content);
            Animation<String> legacyAnim = ScrollAnimation.of(legacy, width, spaceBetween);
            
            this.animation = new Animation<Component>(this.content) {
                @Override
                public Component fetchNextChange() {
                    String animatedLegacy = legacyAnim.fetchNextChange();
                    return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(animatedLegacy);
                }
            };
            return this;
        }
        
        /**
         * Add a custom animation
         */
        public AdventureLineBuilder withAnimation(Animation<Component> customAnimation) {
            this.animation = customAnimation;
            return this;
        }
        
        /**
         * Build the Line instance
         */
        public LineImplementation.AdventureLine build() {
            LineImplementation.AdventureLine line = new LineImplementation.AdventureLine(content, -1);
            if (animation != null) {
                line.setAnimation(animation);
            }
            return line;
        }
    }
    
    @Getter
    @Setter
    class LineImplementation<T> implements Line<T>{
        private T content;
        private int index;
        private Animation<T> animation;
        
        public LineImplementation(T content, int index) {
            this.content = content;
            this.index = index;
            this.animation = null;
        }
        
        public static class LegacyLine extends LineImplementation<String>{
            public LegacyLine(String content, int index) {
                super(content, index);
            }
        }
        
        public static class AdventureLine extends LineImplementation<Component>{
            public AdventureLine(Component content, int index) {
                super(content, index);
            }
        }
    }
}