package dev.mqzen.boards;

import dev.mqzen.boards.base.BoardAdapter;
import dev.mqzen.boards.base.BoardBase;
import dev.mqzen.boards.base.BoardUpdate;
import dev.mqzen.boards.base.impl.LegacyBoard;
import dev.mqzen.boards.base.impl.AdventureBoard;
import dev.mqzen.boards.util.FastReflection;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * A manager class to hold the created boards for players online
 * and also to update them in a scheduled task
 *
 * @since 1.0
 * @author Mqzen (aka Mqzn)
 */
public final class BoardManager {

	private final @NonNull Plugin plugin;
	private @Nullable Integer updateTaskId = null;
	private final @NonNull Map<UUID, BoardBase<?>> boards = new HashMap<>();
	public static final boolean ADVENTURE_SUPPORT;
	private final @Getter Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	static {
		ADVENTURE_SUPPORT = FastReflection
				.optionalClass("io.papermc.paper.adventure.PaperAdventure")
				.isPresent();
	}

	private @Getter long updateInterval = 3L; // in ticks
	private BoardManager(@NonNull Plugin plugin) {
		this.plugin = plugin;
	}

	private static @Nullable BoardManager instance;

	/**
	 * Loads the BoardManager instance into memory
	 * since the class follows The Singleton pattern
	 * there will be only copy of it's instance in memory
	 *
	 * @param plugin the plugin that's using mBoard
	 */
	public static void load(Plugin plugin) {
		if(plugin == null) return;
		instance = new BoardManager(plugin);
	}

	/**
	 * Fetches the loaded instance of the BoardManager
	 * if the instance hasn't been loaded yet by calling 'BoardManager.load(plugin)'
	 * then it will throw an exception
	 *
	 * @throws IllegalStateException when the instance hasn't been loaded yet by calling 'BoardManager.load(plugin)'
	 *
	 * @return the instance loaded :D
	 */

	public static @NonNull BoardManager getInstance() {
		if(instance == null)
			throw new IllegalStateException("BoardManager instance is not initialized correctly," +
							" please try calling the method BoardManager#load");

		return instance;
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
	public @Nullable <T> BoardBase<T> getBoard(@NonNull UUID uuid) throws ClassCastException {
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
	public void setupNewBoard(Player player, BoardAdapter adapter) {
		BoardBase<?> board = ADVENTURE_SUPPORT ? new AdventureBoard(player, adapter) : new LegacyBoard(player, adapter);
		registerBoard(player.getUniqueId(), board);
	}

	/**
	 * This deletes the board created for the player
	 * and unregister it from memory
	 *
	 * @param player the owner of a board.
	 */
	public void removeBoard(@NonNull Player player) {
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
	 * @see BoardManager#setUpdateInterval(long)
	 */
	public void startBoardUpdaters() {
		updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, ()-> {
			for(BoardBase<?> board : boards.values()) {
				if(board.isDeleted())continue;
				BoardUpdate update = board.getUpdate();
				if(update == null) continue;
				try {
					update.update(board);
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}, 1L, updateInterval).getTaskId();
	}

	/**
	 * Stops the scheduled task for board updates
	 * Seemed useless to me but thought perhaps someone
	 * may get a use of it in the future lol
	 *
	 * @see BoardManager#startBoardUpdaters()
	 */
	public void stopBoardUpdaters() {
		if(updateTaskId != null)
			Bukkit.getScheduler().cancelTask(updateTaskId);
	}


}
