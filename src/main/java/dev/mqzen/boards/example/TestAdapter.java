package dev.mqzen.boards.example;

import dev.mqzen.boards.base.BoardAdapter;
import dev.mqzen.boards.base.Title;
import dev.mqzen.boards.base.animation.core.Animation;
import dev.mqzen.boards.base.body.Body;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class TestAdapter implements BoardAdapter {
	Animation<String> titleAnimation = new Animation<>("&4&lmBoard",
					"&4&lmBoard",
					"&c&lm&4&lBoard",
					"&4&lm&c&lB&4&loard",
					"&4&lmB&c&lo&4&lard",
					"&4&lmBo&c&la&4&lrd",
					"&4&lmBoa&c&lr&4&ld",
					"&4&lmBoar&c&ld"
	);

	Animation<String> yourWebsite = new Animation<>("      &eYourWebsite.com      ",
					"     &eYourWebsite.com ",
					"    &eYourWebsite.com",
					"   &eYourWebsite.com",
					" &eYourWebsite.com",
					"&eYourWebsite.com      ",
					"&eourWebsite.com      ",
					"&eurWebsite.com ",
					"&erWebsite.com  ",
					"&eWebsite.com   ",
					"&eebsite.com    ",
					"&ebsite.com     ",
					"&esite.com      ",
					"&eite.com       ",
					"&ete.com        ",
					"&ee.com         ",
					"&e.com          ",
					"&ecom           ",
					"&eom         ",
					"&em             ",
					"&r              ",
					"&r              ",
					"                   &eYo",
					"                 &eYour",
					"              &eYourWe ",
					"            &eYourWebs ",
					"          &eYourWebsit ",
					"        &eYourWebsite. ",
					"       &eYourWebsite.c ",
					"      &eYourWebsite.co ",
					"     &eYourWebsite.com ");


	@Override
	public @NonNull Title title(Player player) {
		return Title.builder()
						.withText("&4&lmBoard")
						.withAnimation(titleAnimation)
						.build();

	}

	@Override
	public @NonNull Body getBody(Player player) {
		Body body = Body.of("&7&l+------------------------+",
						"",
						"&8> &eThis is mBoard,say Hello",
						"");
		body.addNewLine(yourWebsite);
		body.addNewLine("&7&l+------------------------+");
		return body;
	}

}



