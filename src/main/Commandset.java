/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.List;
import java.io.IOException;

import entities.Stuff;
import entities.Player;
import entities.Room;

/** Commands you can enact; different commands may be given to you in the same
 Connection (controlled by Level.) Eg, when you start, you have limited options
 (Newbie,) but once you have a body, you can do much more (Common) but some
 players are able to shutdown the mud, create things, etc (Immortal.)
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public class Commandset {

	private static final int minName  = 3;
	private static final int maxName  = 8;
	private static final int maxUpper = 2;

	/* these are the commands! fixme: anonymous f'n */
	
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
		/* this is where int, wis, are calculated; not that we have them */
		/* fixme: you can't have two names the same? */

		int len = arg.length();
		if(len < minName) {
			c.sendTo("Your name must be at least " + minName + " characters.");
			return;
		}
		if(len > maxName) {
			c.sendTo("Your name must be bounded by " + maxName + " characters.");
			return;
		}

		boolean isUpper, isLastUpper = false;
		boolean isFirst     = true;
		int     upper       = 0;

		for(char ch : arg.toCharArray()) {
			/*Character.isAlphabetic(), isTitleCase; <- sorry the rest of the world */
			if(!Character.isLetter(ch)) {
				c.sendTo("Your name can only be letters.");
				return;
			}
			isUpper = !Character.isLowerCase(ch);
			if(isUpper) upper++;
			if(isFirst && !isUpper
			   || isLastUpper && isUpper
			   || upper > maxUpper) {
				c.sendTo("Appropriate capitalisation please.");
				return;
			}
			isFirst = false;
			isLastUpper = isUpper;
		}
		/* fixme: compare file of bad names (like ***k and such) */
		/* fixme: compare with other players! */

		/* passed the grammar police */
		Player p = new Player(c, arg);
		c.setPlayer(p);
		System.err.print(c + " has created " + arg + ".\n");
		c.sendTo("You create a character named " + arg + "!");

		Room r = c.getMud().getUniverse();
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
				add("",     "look"); /* <-- this */
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

		/* break the string up (fixme: I suppose would could be pedantic and
		 make it any white space; more difficult) */
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

}
