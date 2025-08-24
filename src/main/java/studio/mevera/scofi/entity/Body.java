package studio.mevera.scofi.entity;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public interface Body<T> {
    
    void addLine(T content);
    
    void addLine(Line<T> line);
    
    List<Line<T>> getLines();
    
    default void setLine(int index, Line<T> line) {
        if(index < 0 || index >= getLines().size()) return;
        // Ensure the line has the correct index
        line.setIndex(index);
        getLines().set(index, line);
    }
    
    static BodyImplementation.LegacyBody legacy(String... lines) {
        return legacy(Arrays.asList(lines));
    }
    
    static BodyImplementation.LegacyBody legacy(List<String> lines) {
        return new BodyImplementation.LegacyBody(lines);
    }
    
    
    static BodyImplementation.AdventureBody adventure(Component... components) {
        return adventure(Arrays.asList(components));
    }
    
    static BodyImplementation.AdventureBody adventure(List<Component> components) {
        return new BodyImplementation.AdventureBody(components);
    }
    
    @Getter
    abstract class BodyImplementation<T> implements Body<T>{
        private final List<Line<T>> lines;
        
        public BodyImplementation() {
            lines = new CopyOnWriteArrayList<>();
        }
        
        public static class LegacyBody extends BodyImplementation<String>{
            
            public LegacyBody(List<String> lines) {
                super();
                for (String line : lines) {
                    addLine(line);
                }
            }
            
            @Override
            public void addLine(String content) {
                int correctIndex = getLines().size();
                getLines().add(Line.legacy(ChatColor.translateAlternateColorCodes('&', content), correctIndex));
            }
            
            @Override
            public void addLine(Line<String> line) {
                // FIXED: Always ensure the line gets the correct index based on its position
                int correctIndex = getLines().size();
                line.setIndex(correctIndex);
                getLines().add(line);
            }
        }
        
        public static class AdventureBody extends BodyImplementation<Component>{
            
            public AdventureBody(List<Component> lines) {
                super();
                for (Component line : lines) {
                    addLine(line);
                }
            }
            
            @Override
            public void addLine(Component content) {
                int correctIndex = getLines().size();
                getLines().add(Line.adventure(content, correctIndex));
            }
            
            @Override
            public void addLine(Line<Component> line) {
                // FIXED: Always ensure the line gets the correct index based on its position
                int correctIndex = getLines().size();
                line.setIndex(correctIndex);
                getLines().add(line);
            }
        }
    }
}