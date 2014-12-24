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

//import main.FourOneFourMud;
import entities.Stuff;
import entities.Player;
import entities.Room;

/** Commands you can enact.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public class Commandset {

	private static final String csetDir = "data/commandsets";
	private static final String csetExt = ".cset";

	private static final String cancelCommand = "~";

	/* sorry, I use a US keyboard and it's difficult to type in accents, etc,
	 when addressing, etc, players in real time; only allow players to have
	 ascii names */
	private static final Pattern namePattern = Pattern.compile("([A-Z][a-z]+('[a-z]+)*){1,3}");
	private static final int minName  = 3;
	private static final int maxName  = 8;

	interface Command { void command(final Connection c, final String arg); }

	/* this is so meta */
	protected static final Map<String, Map<String, Command>> commandsets;

	protected static final Map<String, Command> newbie2;
	protected static final Map<String, Command> common2;
	protected static final Map<String, Command> immortal2;

	/* these are the commands; cute, indentation on XCode3 is so confused by
	 lambda expressions; sigh */

	private static final Command help = (c, arg) -> {
		Map<String, Command> commandset = c.getC();
		c.sendTo("yes->These are the commands which you are authorised to use right now:");
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
			p.sendTo("You can't pick " + target + " up.");
			p.sendToRoomExcept(target, p + " tries to pick up " + target + " and fails.");
			target.sendTo(p + " tries to pick you up off the ground and fails.");
			return;
		}
		/* fixme: mass */
		target.placeIn(p);
		p.sendTo("You pick up " + target + ".");
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
		p.sendToRoom("A glorious light surronds " + p + " as they ascend.");
		c.setImmortal();
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
		/*if(!dir.exists() || !dir.isDirectory()) throw new IOException("<" + csetDir + "> is not a thing");*/

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

		Map<String, Command> newbieMod   = new HashMap<String, Command>();
		Map<String, Command> commonMod   = new HashMap<String, Command>();
		Map<String, Command> immortalMod = new HashMap<String, Command>();

		newbieMod.put("help", help);
		newbieMod.put("exit", exit);

		newbie2   = Collections.unmodifiableMap(newbieMod);
		common2   = Collections.unmodifiableMap(newbieMod);
		immortal2 = Collections.unmodifiableMap(newbieMod);

	}

	/* these are the commands! fixme: anonymous f'n */

/*	private static final Command help = (c, arg) -> {
		Commandset set = c.getCommandset();
		c.sendTo("These are the commands which you are authorised to use right now:");
		for (Map.Entry<String, Method> entry : set.commands.entrySet()) {
			c.sendTo(entry.getKey());
		}
	};*/

	private static void help(final Connection c, final String arg) {
		Commandset set = c.getCommandset();
		c.sendTo("These are the commands which you are authorised to use right now:");
		for (Map.Entry<String, Method> entry : set.commands.entrySet()) {
			c.sendTo(entry.getKey()/* + ":" + entry.getValue()*/);
		}
	}

	private static void exit(final Connection c, final String arg) {
		//System.err.print(c + " has exited.\n");
		Player p = c.getPlayer();
		if(p != null) p.sendToRoom(p + " has suddenly vashished.");
		c.sendTo("Goodbye.");
		c.setExit();
	}

	private static void say(final Connection c, final String arg) {
		Player p = c.getPlayer();
		if(p == null) return;
		//System.out.print(c + ": " + arg + "\n");
		c.sendTo("You say, \"" + arg + "\"");
		p.sendToRoom(p + " says \"" + arg + "\"");
	}

	private static void chat(final Connection c, final String arg) {
		Player p = c.getPlayer();
		if(p == null) return;
		// fixme: this is kind of tricky
		String s = "[chat] " + p + ": " + arg;
		for(Connection everyone : c.getMud()) {
			//if(c == everyone) continue; <- echo to ones self is useful
			everyone.sendTo(s);
		}
	}

	private static void take(final Connection c, final String arg) {
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
			p.sendTo("You can't pick " + target + " up.");
			p.sendToRoomExcept(target, p + " tries to pick up " + target + " and fails.");
			target.sendTo(p + " tries to pick you up off the ground and fails.");
			return;
		}
		/* fixme: mass */
		target.placeIn(p);
		p.sendTo("You pick up " + target + ".");
		p.sendToRoomExcept(target, p + " picks up " + target + ".");
		target.sendTo(p + " picks you up.");
	}

	private static void inventory(final Connection c, final String arg) {
		Player p = c.getPlayer();
		if(p == null) return;
		for(Stuff i : p) {
			c.sendTo("" + i);
		}
	}

	private static void cant(final Connection c, final String arg) {
		c.sendTo("You can't do that, [yet.]");
	}

	private static void create(final Connection c, final String arg) {

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
		System.err.print(c + " has created " + arg + ".\n");
		c.sendTo("You create a character named " + arg + "!");

		Room r = c.getMud().getHome();
		p.transportTo(r);
	}
	
	private static void look(final Connection c, final String arg) {
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
				/* look at the Stuff */
				p.lookAtStuff();
			} else {
				c.sendTo("You are floating in space.");
			}
		}
	}

	private static void shutdown(final Connection c, final String arg) {

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
	}

	private static void ascend(final Connection c, final String arg) {
		Player p = c.getPlayer();
		if(p == null) {
			c.sendTo("You must have a body.");
			return;
		}
		if(!c.getMud().comparePassword(arg)) {
			c.sendTo("That's not the password.");
			return;
		}
		p.sendToRoom("A glorious light surronds " + p + " as they ascend.");
		c.setImmortal();
		System.err.print(c + " has ascended.\n");
		c.sendTo("You are now an immortal; type 'help' for new commands.");
	}

	private static void north(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.N);
	}

	private static void east(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.E);
	}

	private static void south(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.S);
	}

	private static void west(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.W);
	}

	private static void up(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.U);
	}

	private static void down(final Connection c, final String arg) {
		Stuff in;
		Player p = c.getPlayer();
		if(p == null) return;
		p.go(Room.Direction.D);
	}

	private static void who(final Connection c, final String arg) {
		Player p;
		c.sendTo("Active connections:");
		for(Connection who : c.getMud()) {
			p = who.getPlayer();
			c.sendTo(who + " (" + (p != null ? p.getName() : "not in game") + ")");
		}
	}

	/* this is the setup for dealing with them */

	public enum Level { NEWBIE, COMMON, IMMORTAL }

	private final Level level;
	private final Map<String, Method> commands = new HashMap<String, Method>();
	private final Map<String, Command> commands2 = newbie2;// = new HashMap<String, Command>();

	public Commandset() {
		System.err.print("Static Commandset activated.\n");
		level = Level.NEWBIE;
	}

	/** Gets a Commandset appropriate to level.
	 @param level	The level, Commandset.Level.{ NEWBIE, COMMON, IMMORTAL }. */
	public Commandset(Level level) {
		this.level = level;

		add("exit", "exit");
		add("quit", "exit");
		add("help", "help");
		add("?", "help");
		add("who", "who");

		/* these are level-specific */
		switch(level) {
			case IMMORTAL:
				add("shutdown", "shutdown");
			case COMMON:
				add("look", "look");
				add("l",    "look");
				add("",     "look"); /* <-- this? */
				add("say",  "say");
				add("'",    "say");
				add("chat", "chat");
				add(".",    "chat");
				if(level != Level.IMMORTAL) add("ascend", "ascend");
				add("n",    "north");
				add("e",    "east");
				add("s",    "south");
				add("w",    "west");
				add("u",    "up");
				add("d",    "down");
				add("north","north");
				add("east", "east");
				add("south","south");
				add("west", "west");
				add("up",   "up");
				add("down", "down");
				add("take", "take");
				add("put",  "cant");
				add("i",    "inventory");
				add("inventory", "inventory");
				break;
			case NEWBIE:
				add("create", "create");
				break;
		}
		test();

		/*Command help = (c, arg) -> {
			Commandset set = this;//c.getCommandset();
			c.sendTo("These are the commands which you are authorised to use right now:");
			for(Map.Entry<String, Command> entry : set.commands2.entrySet()) {
				c.sendTo(entry.getKey());
			}
		};
		commands2.put("help", help);*/
		// and then make it unmodifyable
	}

	private void add(final String command, final String method) {
		try {
			commands.put(command, this.getClass().getDeclaredMethod(method, Connection.class, String.class));
		} catch(NoSuchMethodException e) {
			System.err.format("%s: %s!\n", this, e);
		}
	}

	/** This parses the string and runs it.
	 @param c		The connection that's attributed the command.
	 @param command	A command to parse. */
	public void interpret(final Connection c, final String command) {
		String cmd, arg;

		//System.err.print(c + " running Command::interpret: " + command + ".\n");

		/* break it off if ~ is at the end */
		if(command.endsWith(cancelCommand)) {
			c.sendTo("Cancelled.");
			return;
		}
		/* break the string up */
		/* fixme: any white space */
		int space = command.indexOf(' ');
		if(space != -1) {
			cmd = command.substring(0, space);
			arg = command.substring(space).trim();
		} else {
			cmd = command;
			arg = "";
		}

		/* parse */
		Method run = commands.get(cmd);
		Command run2 = commands2.get(cmd);

		if(run2 != null) {
			run2.command(c, arg);
		}

		/* run */
		if(run == null) {
			c.sendTo("Huh? \"" + cmd + "\" (use help for a list)");
		} else {
			try {
				run.invoke(null, c, arg);
			} catch(Exception e) {
				c.sendTo(command + ": there was a slight problem with doing that.");
				System.err.print(c + " input '" + command + "' which: " + e + ".\n");
			}
		}
	}

	/** @return Synecdoche. */
	public String toString() {
		return "CommandSet " + level;
	}


	public void test() {
		Runnable r = () -> {
			System.out.print("yowassup\n");
		};
		r.run();
		Command c = (a, b) -> {
			System.out.print("yowassup " + b + "\n");
		};
		c.command(null, "foo");
	}

}
