package dev.mqzen.boards.base;

import dev.mqzen.boards.base.body.Body;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface BoardAdapter {


	/**
	 * Fetches the title to be represented
	 * on the board that has this adapter instance;
	 *
	 * @param player the player who will view the title
	 * @return the title of the scoreboard
	 */
	@NonNull Title title(Player player);

	/**
	 * Gets the body to be represented
	 * as the body of the scoreboard
	 * which will occupy this adapter as it's
	 * model or template to take data from.
	 *
	 * @param player the player who will view the lines
	 * @return the body of the scoreboard
	 */
	@NonNull Body getBody(Player player);

	/**
	 * Returns an update action if
	 * the board has any type of animations
	 * this is recommended to implement and return
	 * your own implementation.However, the best implementation recommendedis this:
	 *
	 *   return (board) -> {
	 * 		board.updateTitle();
	 * 		board.updateBody();
	 *   };
	 *
	 *
	 * @return the actions to be executed as an update to the board
	 */
	@Nullable default BoardUpdate getBoardUpdate() {
		return null;
	}

}
