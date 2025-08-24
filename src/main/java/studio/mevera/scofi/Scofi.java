package studio.mevera.scofi;

import studio.mevera.scofi.base.*;
import studio.mevera.scofi.base.impl.LegacyBoard;
import studio.mevera.scofi.base.impl.AdventureBoard;
import studio.mevera.scofi.util.FastReflection;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A manager class to hold the created boards for players online
 * and also to update them in a scheduled task
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 */
public class Scofi {

	private final @NotNull Plugin plugin;
	private @Nullable Integer updateTaskId = null;
	private final @NotNull Map<UUID, BoardBase<?>> boards = new HashMap<>();
	public static final boolean ADVENTURE_SUPPORT;
	private final @Getter Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	static {
		ADVENTURE_SUPPORT = FastReflection
				.optionalClass("io.papermc.paper.adventure.PaperAdventure")
				.isPresent();
	}

	private @Getter long updateInterval = 3L; // in ticks
	private Scofi(@NotNull Plugin plugin) {
		this.plugin = plugin;
	}


	/**
	 * Loads the Scofi instance into memory
	 * since the class follows The Singleton pattern
	 * there will be only copy of it's instance in memory
	 *
	 * @param plugin the plugin that's using mBoard
	 */
	public static Scofi load(Plugin plugin) {
		if(plugin == null )
			throw new IllegalArgumentException("Plugin cannot be null");
		return new Scofi(plugin);
	}

	/**
	 * Sets the update interval of the boards
	 * @param interval the interval in ticks
	 */
	public void setUpdateInterval(long interval) {
		if(updateTaskId == null) {
			this.updateInterval = interval;
			return;
		}
		Bukkit.getScheduler().cancelTask(updateTaskId);
		this.updateInterval = interval;
		this.startBoardUpdaters();
	}

	/**
	 * Fetches the board created for the player
	 * whose uuid matches that of the parameter
	 *
	 * @param uuid the uuid of the player who is
	 *             the owner of a board
	 *
	 * @return the board made for that player
	 * returns null if the player has no board registered !
	 */
	@SuppressWarnings("unchecked")
	public @Nullable <T> BoardBase<T> getBoard(@NotNull UUID uuid) throws ClassCastException {
		return (BoardBase<T>) boards.get(uuid);
	}

	/**
	 * Registers a board for a player's uuid
	 * @param uuid the uuid of the player to register the board for.
	 * @param mBoard the board to be registered for that uuid
	 */
	private void registerBoard(UUID uuid, BoardBase<?> mBoard) {
		boards.put(uuid, mBoard);
	}

	/**
	 * Creates a new board and registers it for the player
	 * using an adapter class that represents some data of the board
	 * that are needed to be obtained
	 *
	 * @param player the player to have the new board created and registered
	 * @param adapter the info carrier of the board
	 */
	public void setupNewBoard(Player player, BoardAdapter<?> adapter) {
		if(adapter instanceof ModernBoardAdapter && !ADVENTURE_SUPPORT) {
			throw new UnsupportedOperationException("Use of modern board adapter is not supported in this mc version.");
		}
		
		BoardBase<?> board;
        if (ADVENTURE_SUPPORT) {
			if (!(adapter instanceof ModernBoardAdapter)) {
				throw new IllegalStateException("You cannot use legacy board adapter in a modern mc version !");
			}
			ModernBoardAdapter modernBoardAdapter = (ModernBoardAdapter)adapter;
            board = new AdventureBoard(this, player, modernBoardAdapter);
        } else {
            board = new LegacyBoard(this, player, (LegacyBoardAdapter) adapter);
        }
		
        registerBoard(player.getUniqueId(), board);
	}

	/**
	 * This deletes the board created for the player
	 * and unregister it from memory
	 *
	 * @param player the owner of a board.
	 */
	public void removeBoard(@NotNull Player player) {
		BoardBase<?> board = getBoard(player.getUniqueId());
		if(board != null) {
			board.delete();
		}
		boards.remove(player.getUniqueId());
	}

	/**
	 * Start the task of the board updates
	 * to allow boards to get updated every certain period
	 *
	 * @see Scofi#setUpdateInterval(long)
	 */
	public void startBoardUpdaters() {
		updateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, ()-> {
			for(BoardBase<?> board : boards.values()) {
				if(board.isDeleted())continue;
				BoardUpdate update = board.getUpdate();
				if(update == null) continue;
				try {
					update.update(board);
				}catch (Exception ex) {
					plugin.getLogger().log(Level.SEVERE, ex, ()-> "Failed to update " + board.getPlayer().getName() + "'s scoreboard.");
				}
			}
		}, 1L, updateInterval).getTaskId();
	}

	/**
	 * Stops the scheduled task for board updates
	 * Seemed useless to me but thought perhaps someone
	 * may get a use of it in the future lol
	 *
	 * @see Scofi#startBoardUpdaters()
	 */
	public void stopBoardUpdaters() {
		if(updateTaskId != null)
			Bukkit.getScheduler().cancelTask(updateTaskId);
	}


}
