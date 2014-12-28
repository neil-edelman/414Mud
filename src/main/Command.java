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
import common.TextReader;

import entities.Stuff;
import entities.Player;
import entities.Room;
import main.Mud;

/** The contract that a command will have. Fixme: Connection->Stuff alows Stuff
 to use commands, too! (although I'd really have to think about bodyless
 connections) */
interface Command { void command(final Stuff s, final String arg); }

/** Command loader.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
class LoadCommands implements Mud.Loader<Map<String, Command>> {

	/** Loads a command set per implementation of Mud.Loader.
	 @param in	The already open {@link common.TextReader}. */
	public Map<String, Command> load(TextReader in) throws ParseException, IOException {
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
				/* null == static */
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

	/* sorry, I use a US keyboard and it's difficult to type in accents, etc,
	 when addressing, etc, players in real time; only allow players to have
	 ascii names */
	private static final Pattern namePattern = Pattern.compile("([A-Z][a-z]+('[a-z]+)*){1,3}");
	private static final int minName  = 3;
	private static final int maxName  = 8;

	private static final int yellDistance = 3;

	protected static final Command help = (s, arg) -> {
		Connection c = s.getConnection();
		if(c == null) {
			/* fixme: default stuffprogs set */
			s.sendTo("You mast have a connection.");
			return;
		}
		Map<String, Command> commandset = c.getCommandset();
		s.sendTo("These are the commands which you are authorised to use right now:");
		for(Map.Entry<String, Command> entry : commandset.entrySet()) {
			c.sendTo(entry.getKey());
		}
	}, exit = (s, arg) -> {
		Connection c = s.getConnection();
		System.err.print(s + " has exited.\n");
		s.sendToRoom(s + " has suddenly vashished.");
		s.sendTo("Goodbye.");
		if(c != null) c.setExit();
	}, say = (s, arg) -> {
		s.sendTo("You say, \"" + arg + "\"");
		s.sendToRoom(s + " says \"" + arg + "\"");
	}, chat = (s, arg) -> {
		Connection c = s.getConnection();
		if(c == null) {
			s.sendTo("You can't without a connection.");
			return;
		}
		/* fixme: channels! */
		String str = "[chat] " + s + ": " + arg;
		for(Connection everyone : c.getMud()) {
			everyone.sendTo(str);
		}
	}, yell = (s, arg) -> {
		Stuff r = s.getIn();
		if(r == null || !(r instanceof Room)) {
			s.sendTo("You can't yell in here.");
			return;
		}
		s.sendTo("You yell \"" + arg + "\"");
		String str = s + " yells from %s-ish, \"%s\" (%d room(s) away.)";
		Mapper.map((Room)r, yellDistance, (room, dist, dir) -> {
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
		Stuff target = null;
		for(Stuff f : r) {
			if(arg.compareTo("" + f) != 0) continue;
			target = f;
			break;
		}
		if(target == null) {
			s.sendTo("You see no <" + arg + "> here.");
			return;
		}
		if(!target.isTransportable()) {
			s.sendTo("You can't pick " + target + " up.");
			s.sendToRoomExcept(target, s + " tries to pick up " + target + " and fails.");
			target.sendTo(s + " tries to pick you up off the ground and fails.");
			return;
		}
		/* fixme: mass */
		target.placeIn(s);
		s.sendTo("You pick up " + target + ".");
		s.sendToRoomExcept(target, s + " picks up " + target + ".");
		target.sendTo(s + " picks you up.");
	}, inventory = (s, arg) -> {
		for(Stuff i : s) {
			s.sendTo("" + i);
		}
	}, cant = (s, arg) -> {
		s.sendTo("You can't do that, [yet.]");
	}, create = (s, arg) -> {
		Connection c = s.getConnection();
		if(c == null) {
			s.sendTo("You must have a connection.");
			return;
		}
		int len = arg.length();
		if(len < minName) {
			c.sendTo("Your name must be at least " + minName + " characters.");
			return;
		} else if(len > maxName) {
			c.sendTo("Your name must be bounded by " + maxName + " characters.");
			return;
		} else if(!namePattern.matcher(arg).matches()) {
			c.sendTo("Your name must match " + namePattern + "; ie, appropriate capitalisation, please.");
			return;
		}
		/* fixme: compare file of bad names */
		/* fixme: compare with other players (maybe?) */
		/* fixme: this is where int, wis, are calculated; not that we have them */

		/* passed the grammar police */
		s.setName(arg);
		s.setTitle(arg + " is here.");
		try {
			c.setCommandset("common");
		} catch(NoSuchElementException e) {
			System.err.format("%s: %s.\n", c, e);
			c.sendTo("There is no command set 'common;' sorry!");
		}
		System.err.format("%s: create <%s>.\n", c, s);
		//c.sendTo("You create a character named " + s + "!");
		s.transportTo(c.getMud().getHome());
	}, look = (s, arg) -> {
		Stuff in = s.getIn();

		Stuff          victim;
		Room.Direction dir;
		Room           room;

		if(arg.length() > 0) {
			/* look at something */
			int count = 0;
			if(in != null) {
				/* look at things in the room */
				if((victim = in.matchContents(arg)) != null) {
					s.sendTo(victim.lookDetailed(s));
					count++;
				}
				/* look at exits */
				if((dir = Room.Direction.find(arg)) != null) {
					if((room = in.getRoom(dir)) != null) {
						s.sendTo(room.look());
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
			if(in != null) {
				s.sendTo(in.lookDetailed(s));
			} else {
				s.sendTo("You are floating in space.");
			}
		}
	}, who = (s, arg) -> {
		Connection c = s.getConnection();
		if(c == null) {
			s.sendTo("You must have a connection.");
			return;
		}
		c.sendTo("Active connections:");
		for(Connection hoo : c.getMud()) {
			c.sendTo(hoo + " (" + hoo.getPlayer().getName() + ")");
		}
	}, shutdown = (s, arg) -> {
		Connection c = s.getConnection();
		if(arg.length() != 0) {
			s.sendTo("Command takes no arguments.");
			return;
		}
		if(c == null) {
			s.sendTo("Must have a connection.");
			return;
		}

		System.out.print(c + " initated shutdown.\n");

		String str = s + " initiated shutdown!";
		for(Connection everyone : c.getMud()) {
			everyone.sendTo(str);
			everyone.setExit(); /* doesn't work -- Connection stuck waiting */
			try {
				everyone.getSocket().close();
			} catch(IOException e) {
				System.err.format("%s just wouldn't close: %s.\n", everyone, e);
			}
		}

		c.setExit();
		c.getMud().shutdown();
	}, ascend = (s, arg) -> {
		Connection c = s.getConnection();
		if(c == null) {
			s.sendTo("You must have a connection.");
			return;
		}
		if(!c.getMud().comparePassword(arg)) {
			c.sendTo("That's not the password.");
			return;
		}
		try {
			c.setCommandset("immortal");
		} catch(NoSuchElementException e) {
			System.err.format("ascend: %s, %s.\n", c, e);
			c.sendTo("No command set immortal exists.");
			return;
		}
		c.sendTo("You are now an immortal; type 'help' for new commands.");
		s.sendToRoom("A glorious light surronds " + s + " as they ascend.");
		System.err.print(c + " has ascended.\n");
}, areas = (s, arg) -> {
Connection c = s.getConnection();
if(c == null) {
s.sendTo("You must have a connection.");
return;
}
Map<String, Area> areas = c.getMud().getAreas();
for(Area a : areas.values()) {
c.sendTo(a.toString());
}
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
			s.sendTo("Not any <" + arg + "> here.");
			return;
		}
		if(!target.isEnterable()) {
			s.sendTo("You can't do that.");
			return;
		}
		//s.enter(target, target instanceof Character ? false/*on*/ : true/*in*/); ???
		s.enter(target, true);
	} /* unmount */;

}
