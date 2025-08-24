package studio.mevera.scofi.base.impl;

import studio.mevera.scofi.Scofi;
import studio.mevera.scofi.animation.core.Animation;
import studio.mevera.scofi.base.BoardBase;
import studio.mevera.scofi.base.BoardUpdate;
import studio.mevera.scofi.base.LegacyBoardAdapter;
import studio.mevera.scofi.entity.Line;
import studio.mevera.scofi.entity.Title;
import studio.mevera.scofi.util.FastReflection;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class LegacyBoard extends BoardBase<String> {
    
    private static final MethodHandle MESSAGE_FROM_STRING;
    private static final Object EMPTY_MESSAGE;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
            MESSAGE_FROM_STRING = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
            EMPTY_MESSAGE = Array.get(MESSAGE_FROM_STRING.invoke(""), 0);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
    
    /**
     * Creates a new FastBoard.
     */
    private final LegacyBoardAdapter adapter;
    
    // Cache for animations to preserve their state
    private Animation<String> cachedTitleAnimation;
    private final Map<Integer, Animation<String>> cachedLineAnimations = new HashMap<>();
    
    public LegacyBoard(Scofi scofi, Player player, LegacyBoardAdapter adapter) {
        super(scofi, player);
        this.adapter = adapter;
        update();
    }
    
    @Override
    public void updateTitle(String title) {
        Objects.requireNonNull(title, "title");
        
        if (!VersionType.V1_13.isHigherOrEqual() && title.length() > 32) {
            throw new IllegalArgumentException("Title is longer than 32 chars");
        }
        
        super.updateTitle(title);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLines(String... lines) {
        Objects.requireNonNull(lines, "lines");
        
        if (!VersionType.V1_13.isHigherOrEqual()) {
            int lineCount = 0;
            for (String s : lines) {
                if (s != null && s.length() > 30) {
                    throw new IllegalArgumentException("Line " + lineCount + " is longer than 30 chars");
                }
                lineCount++;
            }
        }
        
        super.updateLines(lines);
    }
    
    @Override
    public BoardUpdate getUpdate() {
        return adapter.getBoardUpdate();
    }
    
    @Override
    protected void sendLineChange(int score) throws Throwable {
        int maxLength = hasLinesMaxLength() ? 16 : 1024;
        String line = getLineByScore(score);
        String prefix;
        String suffix = "";
        
        if (line == null || line.isEmpty()) {
            prefix = COLOR_CODES[score] + ChatColor.RESET;
        } else if (line.length() <= maxLength) {
            prefix = line;
        } else {
            // Prevent splitting color codes
            int index = line.charAt(maxLength - 1) == ChatColor.COLOR_CHAR
                    ? (maxLength - 1) : maxLength;
            prefix = line.substring(0, index);
            String suffixTmp = line.substring(index);
            ChatColor chatColor = null;
            
            if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
                chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
            }
            
            String color = ChatColor.getLastColors(prefix);
            boolean addColor = chatColor == null || chatColor.isFormat();
            
            suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET.toString() : color) : "") + suffixTmp;
        }
        
        if (prefix.length() > maxLength || suffix.length() > maxLength) {
            // Something went wrong, just cut to prevent client crash/kick
            prefix = prefix.substring(0, Math.min(maxLength, prefix.length()));
            suffix = suffix.substring(0, Math.min(maxLength, suffix.length()));
        }
        
        sendTeamPacket(score, TeamMode.UPDATE, prefix, suffix);
    }
    
    @Override
    protected Object toMinecraftComponent(String line) throws Throwable {
        if (line == null || line.isEmpty()) {
            return EMPTY_MESSAGE;
        }
        
        return Array.get(MESSAGE_FROM_STRING.invoke(line), 0);
    }
    
    @Override
    protected String serializeLine(String value) {
        return value;
    }
    
    @Override
    protected String emptyLine() {
        return "";
    }
    
    @Override
    public boolean update() {
        // Get new title and body from adapter (for dynamic content)
        Title<String> newTitle = adapter.getTitle(getPlayer());
        
        // Handle title animation caching
        if (newTitle.loadAnimation().isPresent()) {
            Animation<String> newTitleAnimation = newTitle.loadAnimation().get();
            
            // If we don't have a cached animation or it's a different animation, cache it
            if (cachedTitleAnimation == null || !isSameAnimation(cachedTitleAnimation, newTitleAnimation)) {
                cachedTitleAnimation = newTitleAnimation;
            } else {
                // Use the cached animation to preserve state
                ((Title.TitleImplementation<String>) newTitle).setTitleAnimation(cachedTitleAnimation);
            }
        } else {
            cachedTitleAnimation = null;
        }
        
        // Update title with preserved animation state
        updateTitle(newTitle.get().orElseThrow(IllegalStateException::new));
        
        // Handle body/lines with animation caching
        for (Line<String> line : adapter.getBody(getPlayer()).getLines()) {
            int index = line.getIndex();
            
            // Handle line animation caching
            if (line.getAnimation() != null) {
                Animation<String> lineAnimation = line.getAnimation();
                Animation<String> cachedAnimation = cachedLineAnimations.get(index);
                
                // If we don't have a cached animation or it's different, cache the new one
                if (cachedAnimation == null || !isSameAnimation(cachedAnimation, lineAnimation)) {
                    cachedLineAnimations.put(index, lineAnimation);
                } else {
                    // Use cached animation to preserve state
                    line.setAnimation(cachedAnimation);
                }
            } else {
                // Remove cached animation if line no longer has one
                cachedLineAnimations.remove(index);
            }
            
            updateLine(index, line.fetchContent());
        }
        
        // Clean up cached animations for lines that no longer exist
        int bodySize = adapter.getBody(getPlayer()).getLines().size();
        cachedLineAnimations.entrySet().removeIf(entry -> entry.getKey() >= bodySize);
        
        return true;
    }
    
    /**
     * Helper method to check if two animations are the "same"
     * (same original content and type)
     */
    private boolean isSameAnimation(Animation<String> cached, Animation<String> newAnim) {
        // Check if they're the same type and have the same original content
        return cached.getClass().equals(newAnim.getClass())
                && Objects.equals(cached.getOriginal(), newAnim.getOriginal());
    }
}