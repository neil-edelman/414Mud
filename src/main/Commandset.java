/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.lang.reflect.Method;
import java.util.List;
import java.io.IOException;

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

/** Commands you can enact; actually part of Connection, but imagine the file
 size! Connection extends Commandset.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public abstract class Commandset {

	private static final String csetDir = "data/commandsets";
	private static final String csetExt = ".cset";

	/* sorry, I use a US keyboard and it's difficult to type in accents, etc,
	 when addressing, etc, players in real time; only allow players to have
	 ascii names */
	private static final Pattern namePattern = Pattern.compile("([A-Z][a-z]+('[a-z]+)*){1,3}");
	private static final int minName  = 3;
	private static final int maxName  = 8;

	private static final int yellDistance = 2;

	/* this is so meta */
	protected static final Map<String, Map<String, Command>> commandsets;

	/* this is where the commands are stored */

	interface Command { void command(final Connection c, final String arg); }

	private static final Command help = (c, arg) -> {
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
			c.sendTo("You yell, but no one can hear you.");
			return;
		}
		c.sendTo("You yell \"" + arg + "\"");
		c.getMapper().map((Room)r, yellDistance, (room, d) -> {
			room.sendToContentsExcept(p, p + " yells \"" + arg + "\"");
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
		} catch(NoSuchFieldException e) {
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

		Stuff surround = p.getIn();

		/* look at the room (Stuff in) */
		if(arg.length() > 0) {
			int count = 0;
			if(surround != null) {
				/* look at things in the room */
				for(Stuff stuff : surround) {
					if(arg.equals(stuff.getName())) {
						c.sendTo(stuff.lookDetailed());
						count++;
					}
				}
				/* look at exits */
				Room.Direction  dir = Room.Direction.find(arg);
				if(dir != null) {
					Room room = surround.getRoom(dir);
					if(room != null) {
						c.sendTo(room.look());
						for(Stuff in : room) c.sendTo(in.look());
					} else {
						c.sendTo("You can't go that way.");
					}
					count++;
				}
			}
			/* look at inventory */
			for(Stuff stuff : p) {
				if(arg.equals(stuff.getName())) {
					c.sendTo(stuff.lookDetailed());
					count++;
				}
			}
			/* fixme: look at eq't */

			if(count == 0) c.sendTo("There is no '" + arg + "' here.");

		} else {
			if(surround != null) {
				c.sendTo(surround.lookDetailed());
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
		} catch(NoSuchFieldException e) {
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
	};

	static {

		Map<String, Map<String, Command>> commandsetsMod = new HashMap<String, Map<String, Command>>();

		File dir = new File(csetDir);
		try {
			if(!dir.exists() || !dir.isDirectory()) throw new IOException("<" + csetDir + "> is not a thing");
		} catch(IOException e) {
			System.err.format("%s.\n", e);
		}

		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return name.endsWith(csetExt);
			}
		});

		/* go though all data/commandsets/*.cset */
		for(File f : files) {

			String name = f.getName();
			name = name.substring(0, name.length() - csetExt.length());

			System.err.format("%s: loading command set <%s>.\n", name, f);

			Map<String, Command> mod = new HashMap<String, Command>();

			try(
				TextReader in = new TextReader(Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8));
			) {
				Scanner scan;
				String line, alias, cmdStr;
				Command command;

				/* go through all the lines of the file, in */
				while((line = in.readLine()) != null) {
					scan = new Scanner(line);
					if((alias = scan.next()) == null) throw new ParseException("alias", in.getLineNumber());
					/*if((thing = .get(scan.next())) == null*/
					if((cmdStr = scan.next()) == null) throw new ParseException("command", in.getLineNumber());
					if(scan.hasNext()) throw new ParseException("too much stuff", in.getLineNumber());
					try {
						command = (Command)Commandset.class.getDeclaredField(cmdStr).get(null);
						if(FourOneFourMud.isVerbose) System.err.format("%s: command <%s>: \"%s\"->%s\n", name, alias, cmdStr, command);
						mod.put(alias, command);
					} catch(NoSuchFieldException | IllegalAccessException e) {
						System.err.format("%s (line %d:) no such command? %s.\n", f, in.getLineNumber(), e);
					}
				}
			} catch(ParseException e) {
				System.err.format("%s; syntax error: %s, line %d.\n", f, e.getMessage(), e.getErrorOffset());
			} catch(IOException/* | NamingException*/ e) {
				System.err.format("%s; %s.\n", f, e);
			}

			commandsetsMod.put(name, Collections.unmodifiableMap(mod));
		}

		commandsets = Collections.unmodifiableMap(commandsetsMod);

	}

	/*private void add(final String command, final String method) {
		try {
			commands.put(command, this.getClass().getDeclaredMethod(method, Connection.class, String.class));
		} catch(NoSuchMethodException e) {
			System.err.format("%s: %s!\n", this, e);
		}
	}*/

}
