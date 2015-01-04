/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.lang.reflect.Method;
import java.util.List;
import java.io.IOException;
import java.util.NoSuchElementException;

import java.util.regex.Pattern;

import java.io.File;
import java.nio.file.Files;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.lang.NullPointerException;

import common.TextReader;
import entities.Stuff;
import entities.Player;
import entities.Room;
import main.Mud;

/** The contract that a command will have. */
@FunctionalInterface
public interface Command { void invoke(final Stuff s, final String arg); }

/** Command loader. This is where the commands are stored as lambdas.
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.0, 2014-11 */
class LoadCommands implements Mud.Loader<Map<String, Command>> {

	/* sorry, I use a US keyboard and it's difficult to type in accents, etc,
	 when addressing, etc, players in real time; only allow players to have
	 ascii names */
	private static final Pattern namePattern = Pattern.compile("([A-Z][a-z]+('[a-z]+)*){1,3}");
	private static final int minName  = 3;
	private static final int maxName  = 8;

	private static final int yellDistance = 3;
	private static final int mapDistance  = 2;

	/* fixme: there are many more in UTF-8 */
	private static final String    vowels = "aeiouyAEIOUY";
	private static StringBuilder testChar = new StringBuilder(1);
	static { testChar.append(" "); } /* replaced with test char every time */

	/** Loads a command set per implementation of Mud.Loader.
	 @param in	The already open {@link common.TextReader}. */
	public Map<String, Command> load(final TextReader in) throws ParseException, IOException {
		Scanner scan;
		String line, alias, cmdStr;
		Command command;
		Map<String, Command> map = new HashMap<String, Command>();

		/* go through all the lines of the file, in */
		while((line = in.readLine()) != null) {
			scan = new Scanner(line);
			if((alias = scan.next()) == null || (cmdStr = scan.next()) == null)
				throw new ParseException("too short", in.getLineNumber());
			if(scan.hasNext())
				throw new ParseException("too long", in.getLineNumber());
			try {
				/* "null" means static; all sorts of Stuff will be calling these
				 commands, so it makes sense to have it be one of the parameters */
				command = (Command)LoadCommands.class.getDeclaredField(cmdStr).get(null);
				if(Mud.isVerbose)
					System.err.format("<%s>\t\"%s\"->%s\n", alias, cmdStr, command);
				map.put(alias, command);
			} catch(NoSuchFieldException | IllegalAccessException e) {
				throw new ParseException("no such command? " + e, in.getLineNumber());
			}
		}
		return map;
	}

	/* these are the static pool of commands; the Command variable name is the
	 second parameter of the .cset file (see {@link load}.) */

	protected static final Command help = (s, arg) -> {
		Map<String, Command> commands = s.getHandler().getCommands();
		s.sendTo("These are the commands which you are authorised to use right now:");
		for(Map.Entry<String, Command> entry : commands.entrySet()) {
			s.sendTo(entry.getKey());
		}
	}, exit = (s, arg) -> {
		System.err.print(s + " has exited.\n");
		s.sendToRoom(s + " has suddenly vashished.");
		s.sendTo("Goodbye.");
		s.getHandler().setExit();
	}, say = (s, arg) -> {
		s.sendTo("You say, \"" + arg + "\"");
		s.sendToRoom(s + " says \"" + arg + "\"");
	}, chat = (s, arg) -> {
		/* fixme: channels! */
		String str = "[chat] " + s + ": " + arg;
		for(Connection everyone : s.getHandler().getMud()) {
			everyone.sendTo(str);
		}
	}, yell = (s, arg) -> {
		Stuff r = s.getIn();
		/* fixme: do like map, escape to the room */
		if(r == null || !(r instanceof Room)) {
			s.sendTo("You can't yell in here.");
			return;
		}
		s.sendTo("You yell \"" + arg + "\"");
		String str = s + " yells from %s-ish, \"%s\" (%d room(s) away.)";
		s.getHandler().getMapper().map((Room)r, yellDistance, (room, dist, dir) -> {
			room.sendToContentsExcept(s, String.format(str, dir.getBack(), arg, dist));
			return true; /* <- want everyone to hear */
		});
	}, take = (s, arg) -> {
		Stuff r = s.getIn();
		if(r == null) {
			s.sendTo("You are in space.");
			return;
		}
		/* fixme: have them be in order, 1.obj 2.obj, this is hacked */
		Stuff target = r.matchContents(arg);
		if(target == null) {
			if(r instanceof Room) {
				s.sendTo("You see no " + arg + " here.");
			} else {
				s.sendTo("You probably want the get off the " + r + " first.");
			}
			s.sendToRoom(s + " tries to find " + an(arg) + " here, but fails.");
			return;
		}
		if(target == s) {
			s.sendTo("You try to pick yourself up, but it's not working.");
			s.sendToRoom(s + " tries to pick themselves up, without success.");
		}
		if(!target.isTransportable()) {
			s.sendTo("You can't pick " + target + " up.");
			s.sendToRoomExcept(target, s + " tries to pick up " + target + " and fails.");
			target.sendTo(s + " tries to pick you up off the ground and fails.");
			return;
		}
		/* fixme: check mass */
		target.placeIn(s);
		s.sendTo("You pick up " + target + ".");
		s.sendToRoomExcept(target, s + " picks up " + target + ".");
		target.sendTo(s + " picks you up.");
	}, drop = (s, arg) -> {
		Stuff inventory, in;
		int l;
		if(arg.length() <= 0) {
			s.sendTo("Drop what?");
			return;
		}
		if((inventory = s.matchContents(arg)) == null) {
			String an = an(arg);
			s.sendTo("You don't seem to be carrying " + an + ".");
			s.sendToRoom(s + " looks though their pockets trying to find " + an + ".");
			return;
		}
		String anItem = an(inventory.toString());
		/* fixme: check if it will go there */
		if((in = s.getIn()) == null) {
			s.sendTo("You drop " + anItem + ", and it floats off into space.");
		} else {
			s.sendTo("You drop " + anItem + ".");
		}
		s.sendToRoom(s + " drops " + anItem + ".");
		inventory.placeIn(in);
	}, inventory = (s, arg) -> {
		for(Stuff i : s) {
			s.sendTo("" + i);
		}
	}, cant = (s, arg) -> {
		s.sendTo("You can't do that, [yet.]");
	}, create = (s, arg) -> {
		int len = arg.length();
		if(len < minName) {
			s.sendTo("Your name must be at least " + minName + " characters.");
			return;
		} else if(len > maxName) {
			s.sendTo("Your name must be at most " + maxName + " characters.");
			return;
		} else if(!namePattern.matcher(arg).matches()) {
			s.sendTo("Your name must match " + namePattern + "; ie, appropriate capitalisation, please.");
			return;
		}
		/* fixme: compare file of bad names */
		/* fixme: compare with other players (maybe?) */
		/* fixme: this is where int, wis, are calculated; not that we have them */

		/* passed the grammar police */
		s.setName(arg);
		s.setTitle(arg + " is here.");
		try {
			s.getHandler().setCommands("common");
		} catch(NoSuchElementException e) {
			System.err.format("%s: %s.\n", s, e);
			s.sendTo("There is no command set 'common;' sorry!");
		}
		System.err.format("%s: create <%s>.\n", s.getHandler(), s);
		//c.sendTo("You create a character named " + s + "!");
		s.levelUp();
		s.transportTo(s.getHandler().getMud().getHome());
	}, look = (s, arg) -> {

		Room           room = s.getRoom();
		Room           otherRoom;
		Stuff          victim;
		Room.Direction dir;

		if(arg.length() > 0) {
			/* look at something */
			int count = 0;
			if(room != null) {
				/* look at things in the room */
				if((victim = room.matchContents(arg)) != null) {
					Stuff in = s.getIn();
					if(room == in) {
						s.sendTo(victim.lookDetailed(s));
					} else {
						s.sendTo("You can't look at this from " + in + ".");
					}
					count++;
				}
				/* look at exits */
				if((dir = Room.Direction.find(arg)) != null) {
					if((otherRoom = room.getRoom(dir)) != null) {
						s.sendTo(otherRoom.look());
					} else {
						s.sendTo("You don't want to go that way.");
					}
					count++;
				}
			}
			/* look at inventory */
			if((victim = s.matchContents(arg)) != null) {
				s.sendTo(victim.lookDetailed(s));
				count++;
			}
			/* fixme: look at eq't */

			if(count == 0) s.sendTo("There is no '" + arg + "' here.");

		} else {
			/* just look in general */
			if(room != null) {
				s.sendTo(room.lookDetailed(s));
			} else {
				s.sendTo("You are floating in space.");
			}
		}
	}, who = (s, arg) -> {
		s.sendTo("Active connections:");
		for(Connection hoo : s.getHandler().getMud()) {
			s.sendTo(hoo + " (" + hoo.getPlayer() + ")");
		}
	}, shutdown = (s, arg) -> {
		if(arg.length() != 0) {
			s.sendTo("Command takes no arguments.");
			return;
		}

		System.out.print(s + " initated shutdown.\n");

		String str = s + " initiated shutdown!";
		for(Connection everyone : s.getHandler().getMud()) {
			everyone.sendTo(str);
			everyone.setExit(); /* doesn't work -- Connection stuck waiting */
			try {
				everyone.getSocket().close();
			} catch(IOException e) {
				System.err.format("%s just wouldn't close: %s.\n", everyone, e);
			}
		}

		s.getHandler().setExit();
		s.getHandler().getMud().shutdown();
	}, ascend = (s, arg) -> {
		if(!s.getHandler().getMud().comparePassword(arg)) {
			s.sendTo("That's not the password.");
			return;
		}
		try {
			s.getHandler().setCommands("immortal");
		} catch(NoSuchElementException e) {
			System.err.format("ascend: %s, %s.\n", s, e);
			s.sendTo("No command set immortal exists.");
			return;
		}
		s.levelUp();
		s.sendTo("You are now an immortal; type 'help' for new commands.");
		s.sendToRoom("A glorious light surronds " + s + " as they ascend.");
		System.err.print(s.getHandler() + " has ascended.\n");
	}, areas = (s, arg) -> {
		Map<String, Area> areas = s.getHandler().getMud().getAreas();
		for(Area a : areas.values()) {
			s.sendTo(a.toString());
		}
	}, map = (s, arg) -> {
		Room in = s.getRoom();
		if(in == null) {
			s.sendTo("Interminable blackness surrounds you.");
			return;
		}
		s.sendTo("You close your eyes and concentrate.");
		s.sendToRoom(s + " closes their eyes and concentrates.");
		/* make a map */
		s.sendTo(s.getHandler().getMapper().mapRooms((Room)in));
	}, north = (s, arg) -> {
		s.go(Room.Direction.N);
	}, east = (s, arg) -> {
		s.go(Room.Direction.E);
	}, south = (s, arg) -> {
		s.go(Room.Direction.S);
	}, west = (s, arg) -> {
		s.go(Room.Direction.W);
	}, up = (s, arg) -> {
		s.go(Room.Direction.U);
	}, down = (s, arg) -> {
		s.go(Room.Direction.D);
	}, mount = (s, arg) -> {
		Stuff room, target;
		if((room = s.getIn()) == null || (target = room.matchContents(arg)) == null) {
			s.sendTo("Not any " + arg + " here.");
			return;
		}
		if(!target.isEnterable()) {
			s.sendTo("You can't do that.");
			return;
		}
		//s.enter(target, target instanceof Character ? false/*on*/ : true/*in*/); ???
		s.enter(target, true);
	}, dismount = (s, arg) -> {
		Stuff mount = s.getIn();
		if(mount == null || !mount.isEnterable()) {
			s.sendTo("You are not mounted on anything.");
			s.sendToRoom(s + " looks like they are dismounting an imaginary horse.");
			return;
		}
		Stuff in = mount.getIn();
		String an = an(mount.toString());
		s.sendTo("You dismount and join " + an + " in " + in + ".");
		s.sendToRoom(s + " dismounts " + an + ".");
		s.placeIn(in);
		s.sendToRoom(s + " dismounts " + an + ".");
	};

	/* lazy */
	static String an(final String str) {
		try {
			testChar.setCharAt(0, str.charAt(0));
			return (vowels.contains(testChar) ? "an " : "a ") + str;
		} catch(NullPointerException | IndexOutOfBoundsException e) {
			return "a " + str;
		}
	}

	/** good one */
	/*private static void an(StringBuilder sb, final String str) {
		try {
			testChar.setCharAt(0, str.charAt(0));
			*//** strpbrk *//*
			sb.append(vowels.contains(testChar) ? "an " : "a ");
			sb.append(str);
		} catch(NullPointerException | IndexOutOfBoundsException e) {
			sb.append("a ");
			sb.append(str);
		}
	}*/

	/** How much Mapper should give to the size. */
	static int mapDepth() {
		return mapDistance;
	}

}
