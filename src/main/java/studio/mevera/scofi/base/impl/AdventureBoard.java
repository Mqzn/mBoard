package studio.mevera.scofi.base.impl;

import studio.mevera.scofi.Scofi;
import studio.mevera.scofi.animation.core.Animation;
import studio.mevera.scofi.base.BoardBase;
import studio.mevera.scofi.base.BoardUpdate;
import studio.mevera.scofi.base.ModernBoardAdapter;
import studio.mevera.scofi.entity.Line;
import studio.mevera.scofi.entity.Title;
import studio.mevera.scofi.util.FastReflection;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static studio.mevera.scofi.Scofi.ADVENTURE_SUPPORT;


@Getter
public class AdventureBoard extends BoardBase<Component> {
    
    private static final MethodHandle COMPONENT_METHOD;
    private static final Object EMPTY_COMPONENT;
    
    static {
        
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        
        try {
            if (ADVENTURE_SUPPORT) {
                Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
                Method method = paperAdventure.getDeclaredMethod("asVanilla", Component.class);
                COMPONENT_METHOD = lookup.unreflect(method);
                EMPTY_COMPONENT = COMPONENT_METHOD.invoke(Component.empty());
            } else {
                Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
                COMPONENT_METHOD = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
                EMPTY_COMPONENT = Array.get(COMPONENT_METHOD.invoke(""), 0);
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
    
    private final ModernBoardAdapter adapter;
    
    // Cache for animations to preserve their state
    private Animation<Component> cachedTitleAnimation;
    private final Map<Integer, Animation<Component>> cachedLineAnimations = new HashMap<>();
    
    public AdventureBoard(Scofi scofi, Player player, ModernBoardAdapter adapter) {
        super(scofi, player);
        this.adapter = adapter;
        
        if (!update()) {
            scofi.getLogger().warning("Hey! Looks like you're using legacy text for your board instead of components," +
                    " legacy text has been automatically converted for now. It is better that you use kyori adventure for modern minecraft.");
        }
    }
    
    @Override
    public BoardUpdate getUpdate() {
        return adapter.getBoardUpdate();
    }
    
    @Override
    protected void sendLineChange(int score) throws Throwable {
        Component line = getLineByScore(score);
        
        sendTeamPacket(score, BoardBase.TeamMode.UPDATE, line, null);
    }
    
    @Override
    protected Object toMinecraftComponent(Component component) throws Throwable {
        if (component == null) {
            return EMPTY_COMPONENT;
        }
        
        // If the server isn't running adventure natively, we convert the component to legacy text
        // and then to a Minecraft chat component
        if (!ADVENTURE_SUPPORT) {
            String legacy = serializeLine(component);
            
            return Array.get(COMPONENT_METHOD.invoke(legacy), 0);
        }
        
        return COMPONENT_METHOD.invoke(component);
    }
    
    @Override
    protected String serializeLine(Component value) {
        return LegacyComponentSerializer.legacySection().serialize(value);
    }
    
    @Override
    protected Component emptyLine() {
        return Component.empty();
    }
    
    @Override
    public boolean update() {
        try {
            // Get new title and body from adapter (for dynamic content)
            Title<Component> newTitle = adapter.getTitle(getPlayer());
            
            // Handle title animation caching
            if (newTitle.loadAnimation().isPresent()) {
                Animation<Component> newTitleAnimation = newTitle.loadAnimation().get();
                
                // If we don't have a cached animation or it's a different animation, cache it
                if (cachedTitleAnimation == null || !isSameAnimation(cachedTitleAnimation, newTitleAnimation)) {
                    cachedTitleAnimation = newTitleAnimation;
                } else {
                    // Use the cached animation to preserve state
                    ((Title.TitleImplementation<Component>) newTitle).setTitleAnimation(cachedTitleAnimation);
                }
            } else {
                cachedTitleAnimation = null;
            }
            
            // Update title with preserved animation state
            updateTitle(newTitle.get().orElseThrow(IllegalStateException::new));
            
            // Handle body/lines with animation caching
            for (Line<Component> line : adapter.getBody(getPlayer()).getLines()) {
                int index = line.getIndex();
                Component content;
                
                // Handle line animation caching
                if (line.getAnimation() != null) {
                    Animation<Component> lineAnimation = line.getAnimation();
                    Animation<Component> cachedAnimation = cachedLineAnimations.get(index);
                    
                    // If we don't have a cached animation or it's different, cache the new one
                    if (cachedAnimation == null || !isSameAnimation(cachedAnimation, lineAnimation)) {
                        cachedLineAnimations.put(index, lineAnimation);
                        line.setAnimation(lineAnimation);
                    } else {
                        // Use cached animation to preserve state
                        line.setAnimation(cachedAnimation);
                    }
                    
                    content = line.fetchContent();
                } else {
                    // Remove cached animation if line no longer has one
                    cachedLineAnimations.remove(index);
                    content = line.getContent();
                }
                
                updateLine(index, content);
            }
            
            // Clean up cached animations for lines that no longer exist
            int bodySize = adapter.getBody(getPlayer()).getLines().size();
            cachedLineAnimations.entrySet().removeIf(entry -> entry.getKey() >= bodySize);
            
            return true;
        } catch (ClassCastException e) {
            // Fallback for legacy text
            for (Line<?> line : adapter.getBody(getPlayer()).getLines()) {
                updateLine(line.getIndex(), deserialize(line.fetchContent()));
            }
            updateTitle(deserialize(adapter.getTitle(getPlayer()).get().orElseThrow(IllegalStateException::new)));
            return false;
        }
    }
    
    private Component deserialize(Object o) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(o.toString());
    }
    
    /**
     * Helper method to check if two animations are the "same"
     * (same original content and type)
     */
    private boolean isSameAnimation(Animation<Component> cached, Animation<Component> newAnim) {
        // Check if they're the same type and have the same original content
        return cached.getClass().equals(newAnim.getClass())
                && Objects.equals(cached.getOriginal(), newAnim.getOriginal());
    }
}