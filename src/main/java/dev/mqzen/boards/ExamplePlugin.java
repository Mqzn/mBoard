package dev.mqzen.boards;

import dev.mqzen.boards.base.BoardAdapter;
import dev.mqzen.boards.base.Title;
import dev.mqzen.boards.base.body.Body;
import dev.mqzen.boards.example.TestAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ExamplePlugin extends JavaPlugin implements Listener {

	private TestAdapter adapter;
	@Override
	public void onEnable() {

		adapter = new TestAdapter();
		Bukkit.getPluginManager().registerEvents(this, this);

		BoardManager.load(this);
		BoardManager.getInstance().setUpdateInterval(5L); //default is 2L
		BoardManager.getInstance().startBoardUpdaters();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		BoardManager.getInstance().setupNewBoard(e.getPlayer(), adapter);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		BoardManager.getInstance().removeBoard(e.getPlayer());
	}

}
