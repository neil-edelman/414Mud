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

/** Commands you can enact; actually part of Connection, but imagine the file
 size! Connection extends Commandset.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
class Commands implements Mud.Loader<Map<String, Commands.Command>> {

	/** Loads a command set */
	public Map<String, Commands.Command> load(TextReader in) throws IOException, ParseException {
		Scanner scan;
		String line, alias, cmdStr;
		Commands.Command command;
		Map<String, Commands.Command> map = new HashMap<String, Commands.Command>();

		/* go through all the lines of the file, in */
		while((line = in.readLine()) != null) {
			scan = new Scanner(line);
			if((alias = scan.next()) == null || (cmdStr = scan.next()) == null)
				throw new ParseException("too short", in.getLineNumber());
			if(scan.hasNext())
				throw new ParseException("too long", in.getLineNumber());
			try {
				/* null == static */
				command = (Commands.Command)Commands.class.getDeclaredField(cmdStr).get(null);
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

	/* this is where the commands are stored */

	public interface Command { void command(final Connection c, final String arg); }

	protected static final Command help = (c, arg) -> {
		Map<String, Command> commandset = c.getCommandset();
		c.sendTo("These are the commands which you are authorised to use right now:");
		for(Map.Entry<String, Command> entry : commandset.entrySet()) {
			c.sendTo(entry.getKey()/* + ":" + entry.getValue()*/);
		}
	}, exit = (c, arg) -> {
		//System.err.print(c + " has exited.\n");
		Player p = c.getPlayer();
		if(p != null) p.sendToRoom(p + " has suddenly vashished.");
		c.sendTo("Goodbye.");
		c.setExit();
	}, say = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		c.sendTo("You say, \"" + arg + "\"");
		p.sendToRoom(p + " says \"" + arg + "\"");
	}, chat = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		/* fixme: channels! */
		String s = "[chat] " + p + ": " + arg;
		for(Connection everyone : c.getMud()) {
			everyone.sendTo(s);
		}
	}, yell = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		Stuff r = p.getIn();
		if(r == null || !(r instanceof Room)) {
			c.sendTo("You can't yell in here.");
			return;
		}
		c.sendTo("You yell \"" + arg + "\"");
		String str = p + " yells from %s-ish, \"%s\" (%d room(s) away.)";
		c.getMapper().map((Room)r, yellDistance, (room, dist, dir) -> {
			room.sendToContentsExcept(p, String.format(str, dir.getBack(), arg, dist));
			return true;
		});
	}, take = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		Stuff r = p.getIn();
		if(r == null) return;
		/* fixme: have them be in order, 1.obj 2.obj, this is hacked */
		Stuff target = null;
		for(Stuff s : r) {
			if(arg.compareTo("" + s) != 0) continue;
			target = s;
			break;
		}
		if(!target.isTransportable()) {
			c.sendTo("You can't pick " + target + " up.");
			p.sendToRoomExcept(target, p + " tries to pick up " + target + " and fails.");
			target.sendTo(p + " tries to pick you up off the ground and fails.");
			return;
		}
		/* fixme: mass */
		target.placeIn(p);
		c.sendTo("You pick up " + target + ".");
		p.sendToRoomExcept(target, p + " picks up " + target + ".");
		target.sendTo(p + " picks you up.");
	}, inventory = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		for(Stuff i : p) {
			c.sendTo("" + i);
		}
	}, cant = (c, arg) -> {
		c.sendTo("You can't do that, [yet.]");
	}, create = (c, arg) -> {
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
		Player p = new Player(c, arg);
		c.setPlayer(p);
		try {
			c.setCommandset("common");
		} catch(NoSuchElementException e) {
			System.err.format("%s: %s.\n", c, e);
			c.sendTo("There is no command set 'common;' sorry!");
		}
		System.err.print(c + " has created " + arg + ".\n");
		c.sendTo("You create a character named " + arg + "!");

		Room r = c.getMud().getHome();
		p.transportTo(r);
	}, look = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) {
			c.sendTo("You don't have eyes yet.");
			return;
		}

		Stuff in = p.getIn();

		Stuff          victim;
		Room.Direction dir;
		Room           room;

		if(arg.length() > 0) {
			/* look at something */
			int count = 0;
			if(in != null) {
				/* look at things in the room */
				if((victim = in.matchContents(arg)) != null) {
					c.sendTo(victim.lookDetailed());
					count++;
				}
				/* look at exits */
				if((dir = Room.Direction.find(arg)) != null) {
					if((room = in.getRoom(dir)) != null) {
						c.sendTo(room.look());
						for(Stuff otherIn : room) c.sendTo(otherIn.look());
					} else {
						c.sendTo("You don't want to go that way.");
					}
					count++;
				}
			}
			/* look at inventory */
			if((victim = p.matchContents(arg)) != null) {
				c.sendTo(victim.lookDetailed());
				count++;
			}
			/* fixme: look at eq't */

			if(count == 0) c.sendTo("There is no '" + arg + "' here.");

		} else {
			/* just look in general */
			if(in != null) {
				c.sendTo(in.lookDetailed());
				p.lookAtStuff();
			} else {
				c.sendTo("You are floating in space.");
			}
		}
	}, who = (c, arg) -> {
		Player p;
		c.sendTo("Active connections:");
		for(Connection hoo : c.getMud()) {
			p = hoo.getPlayer();
			c.sendTo(hoo + " (" + (p != null ? p.getName() : "not in game") + ")");
		}
	}, shutdown = (c, arg) -> {
		if(arg.length() != 0) {
			c.sendTo("Command takes no arguments.");
			return;
		}

		Player p = c.getPlayer();
		if(p == null) return;

		System.out.print(c + " initated shutdown.\n");

		String s = p + " initiated shutdown!";
		for(Connection everyone : c.getMud()) {
			everyone.sendTo(s);
			everyone.setExit(); /* doesn't work -- Connection stuck waiting */
			try {
				everyone.getSocket().close();
			} catch(IOException e) {
				System.err.format("%s just wouldn't close: %s.\n", everyone, e);
			}
		}

		c.setExit();
		c.getMud().shutdown();
	}, ascend = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) {
			c.sendTo("You must have a body.");
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
		p.sendToRoom("A glorious light surronds " + p + " as they ascend.");
		System.err.print(c + " has ascended.\n");
		c.sendTo("You are now an immortal; type 'help' for new commands.");
	}, north = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.N);
	}, east = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.E);
	}, south = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.S);
	}, west = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.W);
	}, up = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.U);
	}, down = (c, arg) -> {
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.D);
	}, mount = (c, arg) -> {
		Player p;
		Stuff room, target;
		if((p = c.getPlayer()) == null || (room = p.getIn()) == null || (target = room.matchContents(arg)) == null) {
			c.sendTo("Not any <" + arg + "> here.");
			return;
		}
		if(!target.isEnterable()) {
			c.sendTo("You can't do that.");
			return;
		}
		p.enter(target, true);
		c.sendTo("unmount to get out");
	} /* unmount */;

}
