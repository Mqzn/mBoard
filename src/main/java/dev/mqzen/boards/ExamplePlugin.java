package dev.mqzen.boards;

import dev.mqzen.boards.example.TestAdapter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	private TestAdapter adapter;
	@Override
	public void onEnable() {

		adapter = new TestAdapter();
		Bukkit.getPluginManager().registerEvents(this, this);

		BoardManager.load(this);
		BoardManager.getInstance().setUpdateInterval(5L); //default is 2L
		BoardManager.getInstance().startBoardUpdaters(this);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		System.out.println("REGISTERING " + e.getPlayer().getName() + " board");
		BoardManager.getInstance().setupNewBoard(e.getPlayer(), adapter);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		BoardManager.getInstance().removeBoard(e.getPlayer());
	}

}
