package dev.mqzen.boards.base;

import dev.mqzen.boards.base.animation.core.Animation;
import dev.mqzen.boards.base.body.Body;
import dev.mqzen.boards.base.body.lines.Line;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class MBoard {

	private static final Map<Class<?>, Field[]> PACKETS = new HashMap<>(8);
	private static final String[] COLOR_CODES = Arrays.stream(ChatColor.values())
					.map(Object::toString)
					.toArray(String[]::new);

	private final @Getter Player player;
	private final @Getter String id;
	private @Getter BoardAdapter adapter;
	private final @Getter Body body = Body.empty();

	private @Getter Title title;

	private @Getter boolean deleted = false;

	private @Nullable @Getter @Setter BoardUpdate update;

	/**
	 * Creates a new MBoard.
	 *
	 * @param player the owner of the scoreboard
	 */
	public MBoard(Player player, BoardAdapter adapter) {

		this.player = Objects.requireNonNull(player, "player");
		this.id = "mBoard-" + Integer.toHexString(ThreadLocalRandom.current().nextInt());

		this.adapter = adapter;
		this.title = adapter.title(player);
		this.update = adapter.getBoardUpdate();


		try {
			sendObjectivePacket(ObjectiveMode.CREATE);
			sendDisplayObjectivePacket();
		} catch (Throwable t) {
			Bukkit.getLogger().log(Level.WARNING, "Unable to create scoreboard with id: " + id + " for " + player.getName() );
			t.printStackTrace();

		}
		updateTitle();
		updateBody();

	}

	public void setAdapter(@Nullable BoardAdapter adapter) {
		if(adapter == null) {
			return;
		}
		this.adapter = adapter;
		this.title = adapter.title(player);
		updateTitle();
		updateBody();
	}


	/**
	 * Update the scoreboard title.
	 *
	 * @throws IllegalArgumentException if the title is longer than 32 chars on 1.12 or lower
	 * @throws IllegalStateException    if {@link #delete()} was call before
	 */
	public void updateTitle() {
		Title newTitle = adapter.title(player);
		if (!VersionType.V1_13.isHigherOrEqual() && title.length() > 32) {
			throw new IllegalArgumentException("Title is longer than 32 chars");
		}

		this.title = newTitle;

		try {
			sendObjectivePacket(ObjectiveMode.UPDATE);
		} catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard title", t);
		}
	}

	/**
	 * Get the specified scoreboard line.
	 *
	 * @param line the line number
	 * @return the line
	 * @throws IndexOutOfBoundsException if the line is higher than {@code size}
	 */
	public Line getLine(int line) {
		checkLineNumber(line, true, false);
		return this.body.getLine(line);
	}

	/**
	 * Update a single scoreboard line.
	 *
	 * @param line the line number
	 * @param text the new line text
	 * @throws IndexOutOfBoundsException if the line is higher than {@link #size() size() + 1}
	 */
	public synchronized void updateLine(int line, String text) {
		checkLineNumber(line, false, true);

		try {
			if (line < size()) {
				this.body.updateLineContent(line, text);

				sendTeamPacket(getScoreByLine(line), TeamMode.UPDATE);
				return;
			}

		  Body newBody = Body.of(this.body);

			if (line > size()) {
				for (int i = size(); i < line; i++) {
					newBody.addNewLine("");
				}
			}

			newBody.addNewLine(text);
			updateBody(newBody);
		} catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard lines", t);
		}
	}

	/**
	 * Remove a scoreboard line.
	 *
	 * @param line the line number
	 */
	public synchronized void removeLine(int line) {
		checkLineNumber(line, false, false);

		if (line >= size()) {
			return;
		}

		Body newBody = Body.of(body);
		newBody.removeLine(line);
		updateBody(newBody);
	}

	/**
	 * Update the lines of the scoreboard
	 *
	 * @param newBody the new scoreboard body
	 * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
	 * @throws IllegalStateException    if {@link #delete()} was call before
	 */
	public synchronized void updateBody(Body newBody) {
		Objects.requireNonNull(newBody, "body");
		checkLineNumber(body.size(), false, true);

		if (!VersionType.V1_13.isHigherOrEqual()) {
			for (Line line : newBody) {
				if (line != null && line.length() > 30) {
					throw new IllegalArgumentException("Line " + line.getIndex() + " is longer than 30 chars");
				}
			}
		}

		Body oldBody = Body.of(this.body);
		this.body.clearAll();
		this.body.addAll(newBody);

		int newBodySize = this.body.size();
		try {
			if (oldBody.size() != newBodySize) {
			  Body oldBodyCopy = Body.of(oldBody);

				if (oldBodyCopy.size() > newBodySize) {
					for (int i = oldBodyCopy.size(); i > newBodySize; i--) {
						sendTeamPacket(i - 1, TeamMode.REMOVE);
						sendScorePacket(i - 1, ScoreboardAction.REMOVE);

						oldBody.removeLine(0);
					}
				} else {
					for (int i = oldBodyCopy.size(); i < newBodySize; i++) {
						sendScorePacket(i, ScoreboardAction.CHANGE);
						sendTeamPacket(i, TeamMode.CREATE);

						Line lineByScore = getLineByScore(i);
						if(lineByScore != null) {
							Line newLine = Line.of(lineByScore.getContent(), oldBody.size() - i);
							oldBody.addLine(newLine);
						}
					}
				}
			}

			for (int i = 0; i < newBodySize; i++) {
				sendTeamPacket(i, TeamMode.UPDATE);
			}
		} catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard lines", t);
		}
	}

	public synchronized void updateBody() {
		Body newBody = adapter.getBody(player);
		//newBody.debug();
		this.updateBody(newBody);
	}

	/**
	 * Get the scoreboard size (the number of lines).
	 *
	 * @return the size
	 */
	public int size() {
		return this.body.size();
	}

	/**
	 * Delete this MBoard, and will remove the scoreboard for the associated player if he is online.
	 * After this, all uses of {@link #updateBody} and {@link #updateTitle} will throws an {@link IllegalStateException}
	 *
	 * @throws IllegalStateException if this was already call before
	 */
	public void delete() {
		try {
			for (int i = 0; i < this.body.size(); i++) {
				sendTeamPacket(i, TeamMode.REMOVE);
			}

			sendObjectivePacket(ObjectiveMode.REMOVE);
		} catch (Throwable t) {
			throw new RuntimeException("Unable to delete scoreboard", t);
		}

		this.deleted = true;
	}

	/**
	 * Return if the player has a prefix/suffix characters limit.
	 * By default, it returns true only in 1.12 or lower.
	 * This method can be overridden to fix compatibility with some versions support plugin.
	 *
	 * @return max length
	 */
	protected boolean hasLinesMaxLength() {
		return !VersionType.V1_13.isHigherOrEqual();
	}

	private void checkLineNumber(int line, boolean checkInRange, boolean checkMax) {
		if (line < 0) {
			throw new IllegalArgumentException("Line number must be positive");
		}

		if (checkInRange && line >= this.body.size()) {
			throw new IllegalArgumentException("Line number must be under " + this.body.size());
		}

		if (checkMax && line >= COLOR_CODES.length - 1) {
			throw new IllegalArgumentException("Line number is too high: " + line);
		}
	}

	private int getScoreByLine(int line) {
		return this.body.size() - line - 1;
	}

	private @Nullable Line getLineByScore(int score) {
		return getLineByScore(this.body, score);
	}

	private @Nullable Line getLineByScore(Body body, int score) {
		int index = body.size() - score - 1;
		return body.getLine(index);
	}

	private void sendObjectivePacket(ObjectiveMode mode) throws Throwable {


		Object packet = ReflectionCache.PACKET_SB_OBJ.invoke();

		setField(packet, String.class, this.id);
		setField(packet, int.class, mode.ordinal());

		if (mode != ObjectiveMode.REMOVE) {
			String titleText = this.title.asText(player).orElse(ChatColor.RESET.toString());
			if(title.hasAnimation()) {
				Animation<String> animation = title.loadAnimation().orElse(null);
				if(animation == null)return;
				titleText = animation.fetchNextChange();
			}
			setComponentField(packet, ChatColor.translateAlternateColorCodes('&', titleText), 1);

			if (VersionType.V1_8.isHigherOrEqual()) {
				setField(packet, ReflectionCache.ENUM_SB_HEALTH_DISPLAY, ReflectionCache.ENUM_SB_HEALTH_DISPLAY_INTEGER);
			}
		} else if (ReflectionCache.VERSION_TYPE == VersionType.V1_7) {
			setField(packet, String.class, "", 1);
		}

		sendPacket(packet);
	}

	private void sendDisplayObjectivePacket() throws Throwable {
		Object packet = ReflectionCache.PACKET_SB_DISPLAY_OBJ.invoke();

		setField(packet, int.class, 1); // Position (1: sidebar)
		setField(packet, String.class, this.id); // Score Name

		sendPacket(packet);
	}

	private void sendScorePacket(int score, ScoreboardAction action) throws Throwable {
		Object packet = ReflectionCache.PACKET_SB_SCORE.invoke();

		setField(packet, String.class, COLOR_CODES[score], 0); // Player Name

		if (VersionType.V1_8.isHigherOrEqual()) {
			setField(packet, ReflectionCache.ENUM_SB_ACTION, action == ScoreboardAction.REMOVE
							? ReflectionCache.ENUM_SB_ACTION_REMOVE : ReflectionCache.ENUM_SB_ACTION_CHANGE);
		} else {
			setField(packet, int.class, action.ordinal(), 1); // Action
		}

		if (action == ScoreboardAction.CHANGE) {
			setField(packet, String.class, this.id, 1); // Objective Name
			setField(packet, int.class, score); // Score
		}

		sendPacket(packet);
	}

	private void sendTeamPacket(int score, TeamMode mode) throws Throwable {
		if (mode == TeamMode.ADD_PLAYERS || mode == TeamMode.REMOVE_PLAYERS) {
			throw new UnsupportedOperationException();
		}

		int maxLength = hasLinesMaxLength() ? 16 : 1024;
		Object packet = ReflectionCache.PACKET_SB_TEAM.invoke();

		setField(packet, String.class, this.id + ':' + score); // Team name
		setField(packet, int.class, mode.ordinal(), ReflectionCache.VERSION_TYPE == VersionType.V1_8 ? 1 : 0); // Update mode




		if (mode == TeamMode.CREATE || mode == TeamMode.UPDATE) {
			Line line = getLineByScore(score);
			String prefix;
			String suffix = null;


			String content;

			if(line != null && line.isAnimated()) {
				Animation<String> animation = line.getAnimation();
				if(animation == null) throw new IllegalStateException("Animation cannot be null here");
				content = ChatColor.translateAlternateColorCodes('&', animation.fetchNextChange());
			}else {
				content = line == null ? "" : line.getContent();
			}

			if (line == null || content.length() == 0) {
				prefix = COLOR_CODES[score] + ChatColor.RESET;
			} else if (content.length() <= maxLength) {
				//MBoard additions here
				prefix = content;
				//Addition end

			} else {
				// Prevent splitting color codes


				int index = content.charAt(maxLength - 1) == ChatColor.COLOR_CHAR ? (maxLength - 1) : maxLength;
				prefix = content.substring(0, index);
				String suffixTmp = content.substring(index);
				ChatColor chatColor = null;

				if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
					chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
				}

				String color = ChatColor.getLastColors(prefix);
				boolean addColor = chatColor == null || chatColor.isFormat();

				suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET.toString() : color) : "") + suffixTmp;
			}

			if (prefix.length() > maxLength || (suffix != null && suffix.length() > maxLength)) {
				// Something went wrong, just cut to prevent client crash/kick
				prefix = prefix.substring(0, maxLength);
				suffix = (suffix != null) ? suffix.substring(0, maxLength) : null;
			}

			if (VersionType.V1_17.isHigherOrEqual()) {
				Object team = ReflectionCache.PACKET_SB_SERIALIZABLE_TEAM.invoke();
				// Since the packet is initialized with null values, we need to change more things.
				setComponentField(team, "", 0); // Display name
				setField(team, ReflectionCache.CHAT_FORMAT_ENUM, ReflectionCache.RESET_FORMATTING); // Color
				setComponentField(team, prefix, 1); // Prefix
				setComponentField(team, suffix == null ? "" : suffix, 2); // Suffix
				setField(team, String.class, "always", 0); // Visibility
				setField(team, String.class, "always", 1); // Collisions
				setField(packet, Optional.class, Optional.of(team));
			} else {
				setComponentField(packet, prefix, 2); // Prefix
				setComponentField(packet, suffix == null ? "" : suffix, 3); // Suffix
				setField(packet, String.class, "always", 4); // Visibility for 1.8+
				setField(packet, String.class, "always", 5); // Collisions for 1.9+
			}

			if (mode == TeamMode.CREATE) {
				setField(packet, Collection.class, Collections.singletonList(COLOR_CODES[score])); // Players in the team
			}
		}

		sendPacket(packet);
	}

	private void sendPacket(Object packet) throws Throwable {
		if (this.deleted) {
			throw new IllegalStateException("This MBoard is deleted");
		}

		if (this.player.isOnline()) {
			Object entityPlayer = ReflectionCache.PLAYER_GET_HANDLE.invoke(this.player);
			Object playerConnection = ReflectionCache.PLAYER_CONNECTION.invoke(entityPlayer);
			ReflectionCache.SEND_PACKET.invoke(playerConnection, packet);
		}
	}

	private void setField(Object object, Class<?> fieldType, Object value) throws ReflectiveOperationException {
		setField(object, fieldType, value, 0);
	}

	private void setField(Object packet,
	                      Class<?> fieldType,
	                      Object value,
	                      int count) throws ReflectiveOperationException {
		int i = 0;
		for (Field field : PACKETS.get(packet.getClass())) {
			if (field.getType() == fieldType && count == i++) {
				field.set(packet, value);
			}
		}
	}

	private void setComponentField(Object packet, String value, int count) throws Throwable {
		if (!VersionType.V1_13.isHigherOrEqual()) {
			setField(packet, String.class, value, count);
			return;
		}

		int i = 0;
		for (Field field : PACKETS.get(packet.getClass())) {
			if ((field.getType() == String.class || field.getType() == ReflectionCache.CHAT_COMPONENT_CLASS) && count == i++) {
				field.set(packet, value.isEmpty() ? ReflectionCache.EMPTY_MESSAGE :
								Array.get(ReflectionCache.MESSAGE_FROM_STRING.invoke(value), 0));
			}
		}
	}

	enum ObjectiveMode {
		CREATE, REMOVE, UPDATE
	}

	enum TeamMode {
		CREATE, REMOVE, UPDATE, ADD_PLAYERS, REMOVE_PLAYERS
	}

	enum ScoreboardAction {
		CHANGE, REMOVE
	}

	enum VersionType {
		V1_7, V1_8, V1_13, V1_17;

		public boolean isHigherOrEqual() {
			return ReflectionCache.VERSION_TYPE.ordinal() >= ordinal();
		}
	}


	private static class ReflectionCache {

		private static final VersionType VERSION_TYPE;
		private static final Class<?> CHAT_COMPONENT_CLASS, CHAT_FORMAT_ENUM;
		private static final Object EMPTY_MESSAGE, RESET_FORMATTING;
		private static final MethodHandle MESSAGE_FROM_STRING, PLAYER_CONNECTION,
						SEND_PACKET, PLAYER_GET_HANDLE;

		// Here are the needed scoreboard packets' constructors
		private static final FastReflection.PacketConstructor PACKET_SB_OBJ, PACKET_SB_DISPLAY_OBJ,
						PACKET_SB_SCORE, PACKET_SB_TEAM, PACKET_SB_SERIALIZABLE_TEAM;

		// Scoreboard enums
		private static final Class<?> ENUM_SB_HEALTH_DISPLAY, ENUM_SB_ACTION;
		private static final Object ENUM_SB_HEALTH_DISPLAY_INTEGER,
						ENUM_SB_ACTION_CHANGE, ENUM_SB_ACTION_REMOVE;

		static {
			try {
				MethodHandles.Lookup lookup = MethodHandles.lookup();

				if (FastReflection.isRepackaged()) {
					VERSION_TYPE = VersionType.V1_17;
				} else if (FastReflection.nmsOptionalClass(null, "ScoreboardServer$Action").isPresent()) {
					VERSION_TYPE = VersionType.V1_13;
				} else if (FastReflection.nmsOptionalClass(null, "IScoreboardCriteria$EnumScoreboardHealthDisplay").isPresent()) {
					VERSION_TYPE = VersionType.V1_8;
				} else {
					VERSION_TYPE = VersionType.V1_7;
				}

				String gameProtocolPackage = "network.protocol.game";
				Class<?> craftPlayerClass = FastReflection.obcClass("entity.CraftPlayer");
				Class<?> craftChatMessageClass = FastReflection.obcClass("util.CraftChatMessage");
				Class<?> entityPlayerClass = FastReflection.nmsClass("server.level", "EntityPlayer");
				Class<?> playerConnectionClass = FastReflection.nmsClass("server.network", "PlayerConnection");
				Class<?> packetClass = FastReflection.nmsClass("network.protocol", "Packet");
				Class<?> packetSbObjClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardObjective");
				Class<?> packetSbDisplayObjClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardDisplayObjective");
				Class<?> packetSbScoreClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardScore");
				Class<?> packetSbTeamClass = FastReflection.nmsClass(gameProtocolPackage, "PacketPlayOutScoreboardTeam");
				Class<?> sbTeamClass = VersionType.V1_17.isHigherOrEqual()
								? FastReflection.innerClass(packetSbTeamClass, innerClass -> !innerClass.isEnum()) : null;
				Field playerConnectionField = Arrays.stream(entityPlayerClass.getFields())
								.filter(field -> field.getType().isAssignableFrom(playerConnectionClass))
								.findFirst().orElseThrow(NoSuchFieldException::new);
				Method sendPacketMethod = Arrays.stream(playerConnectionClass.getMethods())
								.filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == packetClass)
								.findFirst().orElseThrow(NoSuchMethodException::new);

				MESSAGE_FROM_STRING = lookup.unreflect(craftChatMessageClass.getMethod("fromString", String.class));
				CHAT_COMPONENT_CLASS = FastReflection.nmsClass("network.chat", "IChatBaseComponent");
				CHAT_FORMAT_ENUM = FastReflection.nmsClass(null, "EnumChatFormat");
				EMPTY_MESSAGE = Array.get(MESSAGE_FROM_STRING.invoke(""), 0);
				RESET_FORMATTING = FastReflection.enumValueOf(CHAT_FORMAT_ENUM, "RESET", 21);
				PLAYER_GET_HANDLE = lookup.findVirtual(craftPlayerClass, "getHandle", MethodType.methodType(entityPlayerClass));
				PLAYER_CONNECTION = lookup.unreflectGetter(playerConnectionField);
				SEND_PACKET = lookup.unreflect(sendPacketMethod);
				PACKET_SB_OBJ = FastReflection.findPacketConstructor(packetSbObjClass, lookup);
				PACKET_SB_DISPLAY_OBJ = FastReflection.findPacketConstructor(packetSbDisplayObjClass, lookup);
				PACKET_SB_SCORE = FastReflection.findPacketConstructor(packetSbScoreClass, lookup);
				PACKET_SB_TEAM = FastReflection.findPacketConstructor(packetSbTeamClass, lookup);
				PACKET_SB_SERIALIZABLE_TEAM = sbTeamClass == null ? null : FastReflection.findPacketConstructor(sbTeamClass, lookup);

				for (Class<?> clazz : Arrays.asList(packetSbObjClass, packetSbDisplayObjClass, packetSbScoreClass, packetSbTeamClass, sbTeamClass)) {
					if (clazz == null) {
						continue;
					}
					Field[] fields = Arrays.stream(clazz.getDeclaredFields())
									.filter(field -> !Modifier.isStatic(field.getModifiers()))
									.toArray(Field[]::new);
					for (Field field : fields) {
						field.setAccessible(true);
					}
					PACKETS.put(clazz, fields);
				}

				if (VersionType.V1_8.isHigherOrEqual()) {
					String enumSbActionClass = VersionType.V1_13.isHigherOrEqual()
									? "ScoreboardServer$Action"
									: "PacketPlayOutScoreboardScore$EnumScoreboardAction";
					ENUM_SB_HEALTH_DISPLAY = FastReflection.nmsClass("world.scores.criteria", "IScoreboardCriteria$EnumScoreboardHealthDisplay");
					ENUM_SB_ACTION = FastReflection.nmsClass("server", enumSbActionClass);
					ENUM_SB_HEALTH_DISPLAY_INTEGER = FastReflection.enumValueOf(ENUM_SB_HEALTH_DISPLAY, "INTEGER", 0);
					ENUM_SB_ACTION_CHANGE = FastReflection.enumValueOf(ENUM_SB_ACTION, "CHANGE", 0);
					ENUM_SB_ACTION_REMOVE = FastReflection.enumValueOf(ENUM_SB_ACTION, "REMOVE", 1);
				} else {
					ENUM_SB_HEALTH_DISPLAY = null;
					ENUM_SB_ACTION = null;
					ENUM_SB_HEALTH_DISPLAY_INTEGER = null;
					ENUM_SB_ACTION_CHANGE = null;
					ENUM_SB_ACTION_REMOVE = null;
				}
			} catch (Throwable t) {
				throw new ExceptionInInitializerError(t);
			}
		}

	}


}