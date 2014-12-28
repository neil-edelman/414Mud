/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.Scanner;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.io.IOException;
import java.util.NoSuchElementException;
import common.UnrecognisedTokenException;

import common.TextReader;
import common.BitVector;
import entities.*;

/** Loading/storing of areas. The area file is composed of three parts:
 a header, for the title, author, home room, (and anything else I've added;)
 a definitions section, <type> <unique id> followed by type-dependent stuff
 (in entities;) and a resets list from the definitions.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
class Area {

	/* area loading enums; wrapping them around BitVector allows efficient lookup */
	private static final BitVector<TypeOfStuff> typeOfStuffFlags = new BitVector<TypeOfStuff>(Area.TypeOfStuff.class);
	private static final BitVector<TypeOfReset> typeOfResetFlags = new BitVector<TypeOfReset>(Area.TypeOfReset.class);

	private String             title;
	private String             author;
	private Map<String, Stuff> stuff;
	private Room               recall;

	/** @return	Default room. */
	public Room getRecall()  { return recall; }

	/** @return	A synecdochical {@link String}. */
	public String toString() { return title + " by " + author; }

	/** Loads an Area.
	 @param in	The already open {@link common.TextReader}. */
	Area(TextReader in) throws ParseException, IOException {
		String recallStr;
		String word, line;
		String id, info = "";
		Scanner     scan;
		TypeOfStuff what;
		Stuff       s = null; /* "variable s might not have been initialized" clever, but no */
		boolean     flags[];
		Map<String, Stuff> modStuff = new HashMap<String, Stuff>();
		Mud.Loader  loader;

		/* grab the header; this.title belongs to Area, not to be confused with
		 the titles of the Stuff in the Area */
		this.title  = in.nextLine();
		this.author = in.nextLine();
		recallStr   = in.nextLine();

		in.assertLine("~");

		/* grab the TypeOfStuff; finished when there's a ~ denoting a it's
		 time for the TypeOfReset */
		while("~".compareTo(line = in.nextLine()) != 0) {
			scan = new Scanner(line);
			if((what = typeOfStuffFlags.find(scan.next())) == null)
				throw new ParseException("unknown token", in.getLineNumber());
			id = scan.next();
			if(scan.hasNext())
				throw new ParseException("too many things", in.getLineNumber());

			/* I replaced a massive switch statement; that's how I roll */
			if((loader = what.loader()) == null)
				throw new ParseException(what + " not implemented", in.getLineNumber());
			s = (Stuff)loader.load(in);
			modStuff.put(id, s);
			if(Mud.isVerbose) {
				System.err.format("%s.%s: name <%s>,  title <%s>.\n", this, id, s.getName(), s.getTitle());
			}
		}
		/* make this a constant */
		stuff = Collections.unmodifiableMap(modStuff);

		/* set the default room now that we've loaded it (hopefully) */
		if((recall = (Room)stuff.get(recallStr)) == null)
			throw new ParseException(recallStr + " not found", in.getLineNumber());

		/* resets/connections to the end of the file */
		Stuff          thing, target;
		TypeOfReset    reset;
		Room.Direction dir;
		while((line = in.readLine()) != null) {
			scan = new Scanner(line);
			if((thing  =       stuff.get(scan.next())) == null)
				throw new ParseException("unknown stuff", in.getLineNumber());
			if((reset  = typeOfResetFlags.find(scan.next())) == null)
				throw new ParseException("unknown token", in.getLineNumber());
			if(reset == TypeOfReset.CONNECT || reset == TypeOfReset.SET) {
				if((dir = Room.Direction.find(scan.next())) == null)
					throw new ParseException("unknown direction", in.getLineNumber());
			} else {
				dir = null;
			}
			if((target =  stuff.get(scan.next())) == null)
				throw new ParseException("unknown argument", in.getLineNumber());
			if(scan.hasNext())
				throw new ParseException("too much stuff", in.getLineNumber());
			reset.invoke(thing, target, dir);

			if(Mud.isVerbose)
				System.err.format("%s: <%s> <%s> direction <%s> to/in <%s>.\n", this, thing, reset, dir, target);
		}

		System.err.format("%s: default room <%s>.\n", this, recall);

	}

	/** An enum of all the types of stuff that we could have in the definitions
	 part of an area. */
	public enum TypeOfStuff {
		CHARACTER("Character",	null),
		CONTAINER("Container",	null),
		MONEY("Money",			null),
		MOB("Mob",				(in) -> { return new Mob(in); }),
		OBJECT("Object",		(in) -> { return new entities.Object(in); }),
		PLAYER("Player",		null),
		ROOM("Room",			(in) -> { return new Room(in); }),
		STUFF("Stuff",			(in) -> { return new Stuff(in); });
		public  String     symbol; /* BitVector requires it to be public */
		private Mud.Loader loader;
		private TypeOfStuff(final String symbol, final Mud.Loader loader) {
			this.symbol = symbol;
			this.loader = loader;
		}
		public String toString()                 { return symbol; }
		public Mud.Loader loader()               { return loader; }
	}

	/** An enum of the types of things that we could have in the reset list part
	 of an area, where the synatax is <TypeOfStuff> <Reset> <Reset.invoke()> */
	public enum TypeOfReset {
		IN("in"),
		CONNECT("connect"),
		SET("set");
		public String symbol; /* BitVector requires it to be public */
		private TypeOfReset(final String symbol)   { this.symbol = symbol; }
		public String toString()                   { return symbol; }
		public void invoke(final Stuff thing, final Stuff arg, final Room.Direction dir) {
			//System.err.format("%s.invoke(%s, %s, %s);\n", this, thing, arg, dir);
			try {
				TypeOfReset.class.getDeclaredMethod(
											  symbol, Stuff.class, Stuff.class, Room.Direction.class
											  ).invoke(this, thing, arg, dir);
			} catch(NoSuchElementException | NoSuchMethodException
					| IllegalAccessException | InvocationTargetException e) {
				System.err.format("Reset %s %s %s, dir %s: %s.\n", thing, symbol, arg, dir, e);
			}
		}
		public void in(final Stuff thing, final Stuff container, final Room.Direction dir) {
			//System.out.format("!!!refl in in %s %s %s\n", thing, container, dir);
			thing.transportTo(container);
		}
		public void connect(final Stuff room, final Stuff target, final Room.Direction dir) {
			//System.out.format("!!!refl connect in %s %s %s\n", room, target, dir);
			Room r = (Room)room;
			r.connectDirection(dir, (Room)target);
		}
		public void set(final Stuff room, final Stuff target, final Room.Direction dir) {
			//System.out.format("!!!refl set in %s %s %s\n", room, target, dir);
			Room r = (Room)room;
			r.setDirection(dir, (Room)target);
		}
	}

}
