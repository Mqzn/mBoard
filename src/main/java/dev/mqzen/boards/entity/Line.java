package dev.mqzen.boards.entity;

import dev.mqzen.boards.animation.core.Animation;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

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
    
    // Original factory methods (kept for backwards compatibility)
    static LineImplementation.LegacyLine legacy(String content, int index){
        return new LineImplementation.LegacyLine(content, index);
    }
    
    static LineImplementation.AdventureLine adventure(Component content, int index){
        return new LineImplementation.AdventureLine(content, index);
    }
    
    // NEW: Factory methods without index (index will be set by Body when added)
    static LineImplementation.LegacyLine legacy(String content){
        return new LineImplementation.LegacyLine(content, -1); // -1 as placeholder, will be set by Body
    }
    
    static LineImplementation.AdventureLine adventure(Component content){
        return new LineImplementation.AdventureLine(content, -1); // -1 as placeholder, will be set by Body
    }
    
    // NEW: Factory methods with animation
    static LineImplementation.LegacyLine legacyAnimated(String content, Animation<String> animation){
        LineImplementation.LegacyLine line = new LineImplementation.LegacyLine(content, -1);
        line.setAnimation(animation);
        return line;
    }
    
    static LineImplementation.AdventureLine adventureAnimated(Component content, Animation<Component> animation){
        LineImplementation.AdventureLine line = new LineImplementation.AdventureLine(content, -1);
        line.setAnimation(animation);
        return line;
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