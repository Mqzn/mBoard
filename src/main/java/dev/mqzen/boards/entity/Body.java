package dev.mqzen.boards.entity;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author Mqzen
 * @Author <a href="https://github.com/Cobeine">Cobeine</a>
 */

public interface Body<T> {
    
    void addLine(T content);
    
    List<Line<T>> getLines();

    default void setLine(int index, Line<T> line) {
        if(index < 0 || index > getLines().size()) return;
        getLines().set(index,line);
    }

    static BodyImplementation.LegacyBody legacy(String... lines) {
        return legacy(List.of(lines));
    }
    
    static BodyImplementation.LegacyBody legacy(List<String> lines) {
        return new BodyImplementation.LegacyBody(lines);
    }
    

    static BodyImplementation.AdventureBody adventure(Component... components) {
            return adventure(List.of(components));
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
                int lastIndex = getLines().size();
                getLines().add(Line.legacy(ChatColor.translateAlternateColorCodes('&',content), lastIndex));
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
                int lastIndex = getLines().size();
                getLines().add(Line.adventure(content, lastIndex));
            }
        }
    }
}
