package studio.mevera.scofi.animation;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class HighLighter {

	private final StringBuilder text;
	private final String primaryColor, secondaryColor;
	private final List<String> highLighted = new ArrayList<>();
	private int position;
	private final int limit;

	private HighLighter(String text, String primaryColor, String secondaryColor) {

		this.text = new StringBuilder(text);
		this.primaryColor = ChatColor.translateAlternateColorCodes('&', primaryColor);
		this.secondaryColor = ChatColor.translateAlternateColorCodes('&', secondaryColor);
		this.limit = text.length();
	}

	private HighLighter(String text, ChatColor primary, ChatColor secondary) {
		this(text, primary.toString(), secondary.toString());
	}

	public String nextResult() {
		StringBuilder builder = new StringBuilder();

		if (position >= limit) {
			position = 0;
		}

		if (position > 0) {
			builder.append(primaryColor).append(text, 0, position);
		}
		String secondaryTarget = text.substring(position, position + 1);
		builder.append(secondaryColor).append(secondaryTarget);

		if (position < limit-1) {
			builder.append(primaryColor).append(text.substring(position+1));
		}

		position++;
		return builder.toString();
	}

	public @NotNull List<String> getHighLighted() {
		return highLighted;
	}

	public static HighLighter of(String text, String primaryColor, String secondaryColor) {
		return new HighLighter(text, primaryColor, secondaryColor);
	}

	public static HighLighter of(String text, ChatColor primary, ChatColor secondary) {
		return new HighLighter(text, primary, secondary);
	}

	public String getHighLighted(int index) {
		return this.highLighted.get(index);
	}

}