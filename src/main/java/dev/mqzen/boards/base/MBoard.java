package dev.mqzen.boards.base;

import dev.mqzen.boards.util.FastReflection;
import dev.mqzen.boards.body.Body;
import dev.mqzen.boards.body.Line;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.print.attribute.Attribute;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class MBoard {

	private static final Map<Class<?>, Field[]> PACKETS = new HashMap<>(8);
	private static final String[] COLOR_CODES = Arrays.stream(ChatColor.values())
					.map(Object::toString)
					.toArray(String[]::new);
	private static final VersionType VERSION_TYPE;
	// Packets and components
	private static final Class<?> CHAT_COMPONENT_CLASS;
	private static final Class<?> CHAT_FORMAT_ENUM;
	private static final Object EMPTY_MESSAGE;
	private static final Object RESET_FORMATTING;
	private static final MethodHandle MESSAGE_FROM_STRING;
	private static final MethodHandle PLAYER_CONNECTION;
	private static final MethodHandle SEND_PACKET;
	private static final MethodHandle PLAYER_GET_HANDLE;
	// Scoreboard packets
	private static final FastReflection.PacketConstructor PACKET_SB_OBJ;
	private static final FastReflection.PacketConstructor PACKET_SB_DISPLAY_OBJ;
	private static final FastReflection.PacketConstructor PACKET_SB_SCORE;
	private static final FastReflection.PacketConstructor PACKET_SB_TEAM;
	private static final FastReflection.PacketConstructor PACKET_SB_SERIALIZABLE_TEAM;
	// Scoreboard enums
	private static final Class<?> ENUM_SB_HEALTH_DISPLAY;
	private static final Class<?> ENUM_SB_ACTION;
	private static final Object ENUM_SB_HEALTH_DISPLAY_INTEGER;
	private static final Object ENUM_SB_ACTION_CHANGE;
	private static final Object ENUM_SB_ACTION_REMOVE;

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

	private final Player player;
	private final String id;

	private final List<Line> lines = new ArrayList<>();
	private final @NonNull @Getter BoardAdapter adapter;
	private final @NonNull AtomicReference<Title> title;

	private final @NonNull AtomicReference<Body> body;
	private boolean deleted = false;

	private @Nullable @Getter @Setter BoardUpdate update;

	/**
	 * Creates a new MBoard.
	 *
	 * @param player the owner of the scoreboard
	 */
	public MBoard(Player player, @NonNull BoardAdapter adapter) {
		this.player = Objects.requireNonNull(player, "player");
		this.id = "fb-" + Integer.toHexString(ThreadLocalRandom.current().nextInt());

		this.adapter = adapter;
		this.title = new AtomicReference<>(adapter.title(player));
		this.body = new AtomicReference<>(adapter.getBody(player));
		this.update = adapter.getBoardUpdate();

		try {
			sendObjectivePacket(ObjectiveMode.CREATE);
			sendDisplayObjectivePacket();
		} catch (Throwable t) {
			throw new RuntimeException("Unable to create scoreboard", t);
		}

		updateTitle();
		updateLines(adapter.getBody(player).getLines());
	}

	/**
	 * Get the scoreboard title.
	 *
	 * @return the scoreboard title
	 */
	public @NonNull Title getTitle() {
		return this.title.get();
	}

	/**
	 * Update the scoreboard title.
	 *
	 * @param title the new scoreboard title
	 * @throws IllegalArgumentException if the title is longer than 32 chars on 1.12 or lower
	 * @throws IllegalStateException    if {@link #delete()} was call before
	 */
	public void updateTitle(@NonNull Title title) {
		Objects.requireNonNull(title, "title");

		if (!VersionType.V1_13.isHigherOrEqual() && title.length() > 32) {
			throw new IllegalArgumentException("Title is longer than 32 chars");
		}

		this.title.set(title);

		try {
			sendObjectivePacket(ObjectiveMode.UPDATE);
		} catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard title", t);
		}

	}

	public void updateTitle() {
		this.updateTitle(title.get());
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
		return this.lines.get(line);
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
				this.lines.set(line, Line.of(text, line));

				sendTeamPacket(getScoreByLine(line), TeamMode.UPDATE);
				return;
			}

			List<Line> newLines = new ArrayList<>(this.lines);

			if (line > size()) {
				for (int i = size(); i < line; i++) {
					newLines.add(Line.empty(i));
				}
			}

			newLines.add(Line.of(text,newLines.size()));

			updateLines(newLines);
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

		List<Line> newLines = new ArrayList<>(this.lines);
		newLines.remove(line);
		updateLines(newLines);
	}

	/**
	 * Update the lines of the scoreboard
	 *
	 * @param newLines the new scoreboard lines
	 * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
	 * @throws IllegalStateException    if {@link #delete()} was call before
	 */
	public synchronized void updateLines(@NonNull List<Line> newLines) {
		Objects.requireNonNull(lines, "lines");
		checkLineNumber(lines.size(), false, true);

		if (!VersionType.V1_13.isHigherOrEqual()) {
			for (Line line : newLines) {
				if (line != null && line.length() > 30) {
					throw new IllegalArgumentException("Line " + line.getIndex() + " is longer than 30 chars");
				}
			}
		}

		List<Line> oldLines = new ArrayList<>(this.lines);
		this.lines.clear();
		this.lines.addAll(newLines);

		int linesSize = this.lines.size();

		try {
			if (oldLines.size() != linesSize) {
				List<Line> oldLinesCopy = new ArrayList<>(oldLines);

				if (oldLines.size() > linesSize) {
					for (int i = oldLinesCopy.size(); i > linesSize; i--) {
						sendTeamPacket(i - 1, TeamMode.REMOVE);
						sendScorePacket(i - 1, ScoreboardAction.REMOVE);

						oldLines.remove(0);
					}
				} else {
					for (int i = oldLinesCopy.size(); i < linesSize; i++) {
						sendScorePacket(i, ScoreboardAction.CHANGE);
						sendTeamPacket(i, TeamMode.CREATE);

						oldLines.add(oldLines.size() - i, getLineByScore(i));
					}
				}
			}

			for (int i = 0; i < linesSize; i++) {
				sendTeamPacket(i, TeamMode.UPDATE);
			}

		} catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard lines", t);
		}
	}

	/**
	 * Update the lines of the scoreboard
	 *
	 * @throws IllegalArgumentException if one line is longer than 30 chars on 1.12 or lower
	 * @throws IllegalStateException    if {@link #delete()} was call before
	 */
	public synchronized void updateLines() {
		/*checkLineNumber(body.size(), false, true);

		if (!VersionType.V1_13.isHigherOrEqual()) {
			for (Line line : body) {
				if (line != null && line.length() > 30) {
					throw new IllegalArgumentException("Line " + line.getIndex() + " is longer than 30 chars");
				}
			}
		}

		try {

			for (int i = 0; i < body.size(); i++) {
				sendTeamPacket(i, TeamMode.UPDATE);
			}

		}catch (Throwable t) {
			throw new RuntimeException("Unable to update scoreboard lines", t);
		}*/
		Body newBody = adapter.getBody(player);
		this.body.set(body.get().copyLineContents(newBody));
		this.updateLines(body.get().getLines());
	}


	/**
	 * Get the player who has the scoreboard.
	 *
	 * @return current player for this MBoard
	 */
	public Player getPlayer() {
		return this.player;
	}

	/**
	 * Get the scoreboard id.
	 *
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Get if the scoreboard is deleted.
	 *
	 * @return true if the scoreboard is deleted
	 */
	public boolean isDeleted() {
		return this.deleted;
	}

	/**
	 * Get the scoreboard size (the number of lines).
	 *
	 * @return the size
	 */
	public int size() {
		return this.lines.size();
	}

	/**
	 * Delete this MBoard, and will remove the scoreboard for the associated player if he is online.
	 * After this, all uses of {@link #updateLines} and {@link #updateTitle} will throws an {@link IllegalStateException}
	 *
	 * @throws IllegalStateException if this was already call before
	 */
	public void delete() {
		try {
			for (int i = 0; i < this.lines.size(); i++) {
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

		if (checkInRange && line >= this.lines.size()) {
			throw new IllegalArgumentException("Line number must be under " + this.lines.size());
		}

		if (checkMax && line >= COLOR_CODES.length - 1) {
			throw new IllegalArgumentException("Line number is too high: " + line);
		}
	}

	private int getScoreByLine(int line) {
		return this.lines.size() - line - 1;
	}

	private Line getLineByScore(int score) {
		return getLineByScore(this.body.get().getLines(), score);
	}

	private Line getLineByScore(List<Line> lines, int score) {
		return lines.get(lines.size() - score - 1);
	}

	private void sendObjectivePacket(ObjectiveMode mode) throws Throwable {
		Object packet = PACKET_SB_OBJ.invoke();

		setField(packet, String.class, this.id);
		setField(packet, int.class, mode.ordinal());

		if (mode != ObjectiveMode.REMOVE) {
			String titleContent;
			val animation = this.title.get().loadAnimation();
			if(animation.isPresent()) {
				titleContent = animation.get().fetchNextChange();
			}else {
				titleContent = title.get().asText(player).orElse(ChatColor.RESET.toString());
			}

			titleContent = ChatColor.translateAlternateColorCodes('&', titleContent);
			setComponentField(packet, titleContent, 1);

			if (VersionType.V1_8.isHigherOrEqual()) {
				setField(packet, ENUM_SB_HEALTH_DISPLAY, ENUM_SB_HEALTH_DISPLAY_INTEGER);
			}
		} else if (VERSION_TYPE == VersionType.V1_7) {
			setField(packet, String.class, "", 1);
		}

		sendPacket(packet);
	}

	private void sendDisplayObjectivePacket() throws Throwable {
		Object packet = PACKET_SB_DISPLAY_OBJ.invoke();

		setField(packet, int.class, 1); // Position (1: sidebar)
		setField(packet, String.class, this.id); // Score Name

		sendPacket(packet);
	}

	private void sendScorePacket(int score, ScoreboardAction action) throws Throwable {
		Object packet = PACKET_SB_SCORE.invoke();

		setField(packet, String.class, COLOR_CODES[score], 0); // Player Name

		if (VersionType.V1_8.isHigherOrEqual()) {
			setField(packet, ENUM_SB_ACTION, action == ScoreboardAction.REMOVE ? ENUM_SB_ACTION_REMOVE : ENUM_SB_ACTION_CHANGE);
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
		Object packet = PACKET_SB_TEAM.invoke();

		setField(packet, String.class, this.id + ':' + score); // Team name
		setField(packet, int.class, mode.ordinal(), VERSION_TYPE == VersionType.V1_8 ? 1 : 0); // Update mode

		if (mode == TeamMode.CREATE || mode == TeamMode.UPDATE) {
			Line line = getLineByScore(score);
			String prefix;
			String suffix = null;

			if(line == null){
				System.out.println("Line is null");
				return;
			}

			String content = ChatColor.translateAlternateColorCodes('&', line.getAnimation() != null ? line.getAnimation().fetchNextChange() : line.getContent());

			if (content.isEmpty()) {
				prefix = COLOR_CODES[score] + ChatColor.RESET;
			} else if (content.length() <= maxLength) {
				prefix = content;
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
				Object team = PACKET_SB_SERIALIZABLE_TEAM.invoke();
				// Since the packet is initialized with null values, we need to change more things.
				setComponentField(team, "", 0); // Display name
				setField(team, CHAT_FORMAT_ENUM, RESET_FORMATTING); // Color
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
			Object entityPlayer = PLAYER_GET_HANDLE.invoke(this.player);
			Object playerConnection = PLAYER_CONNECTION.invoke(entityPlayer);
			SEND_PACKET.invoke(playerConnection, packet);
		}
	}

	private void setField(Object object, Class<?> fieldType, Object value) throws ReflectiveOperationException {
		setField(object, fieldType, value, 0);
	}

	private void setField(Object packet, Class<?> fieldType, Object value, int count) throws ReflectiveOperationException {
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
			if ((field.getType() == String.class || field.getType() == CHAT_COMPONENT_CLASS) && count == i++) {
				field.set(packet, value.isEmpty() ? EMPTY_MESSAGE : Array.get(MESSAGE_FROM_STRING.invoke(value), 0));
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
			return VERSION_TYPE.ordinal() >= ordinal();
		}

	}

}