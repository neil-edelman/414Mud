/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.NoSuchElementException;

import java.util.Map;
import java.util.HashMap;

import java.lang.String;

import java.util.Scanner;
import java.util.Collections;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import common.TextReader;
import common.BitVector;
import common.UnrecognisedTokenException;
import java.text.ParseException;
import javax.naming.NamingException;

import entities.*;

/** Loading/storing of areas. 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
class Area {

	/**** all areas ****/

	private static final String areaExt = ".area";

	/** Load all areas in specified directory.
	 @param strdir	Where the areas are stored. */
	public static Map<String, Area> loadAreas(final String strdir) throws IOException {
		Map<String, Area> areas = new HashMap<String, Area>();
		Area area;
		File dir = new File(strdir);
		if(!dir.exists() || !dir.isDirectory()) {
			throw new IOException("loadArea: <" + strdir + "> is not a thing");
		}
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return name.endsWith(areaExt);
			}
		});
		for(File f : files) {
			try {
				System.err.format("Loading area <%s>.\n", f);
				area = new Area(f);
				areas.put("" + area, area);
			} catch(ParseException e) {
				System.err.format("%s; syntax error: %s, line %d.\n", f, e.getMessage(), e.getErrorOffset());
			} catch(IOException | NamingException e) {
				System.err.format("%s; %s.\n", f, e);
			}
		}
		return areas;
	}

	/**** enums ****/

	public enum TypeOfStuff {
		CHARACTER("Character"),
		CONTAINER("Container"),
		MONEY("Money"),
		MOB("Mob"),
		OBJECT("Object"),
		PLAYER("Player"),
		ROOM("Room"),
		STUFF("Stuff");
		public String symbol;
		private TypeOfStuff(final String symbol) { this.symbol = symbol; }
		public String toString()                 { return symbol; }
	}

	public enum Reset {
		IN("in"),
		CONNECT("connect"),
		SET("set");
		public String symbol;
		private Reset(final String symbol)         { this.symbol = symbol; }
		public String toString()                   { return symbol; }
		public void invoke(final Stuff thing, final Stuff arg, final Room.Direction dir) {
			//System.err.format("%s.invoke(%s, %s, %s);\n", this, thing, arg, dir);
			try {
				Reset.class.getDeclaredMethod(symbol, Stuff.class, Stuff.class, Room.Direction.class).invoke(this, thing, arg, dir);
			} catch(NoSuchElementException | NoSuchMethodException
					| IllegalAccessException | InvocationTargetException e) {
				assert(true): "something's terribly wrong with resets";
				System.err.format("%s: could not call on %s, %s, %s.\n", thing, arg, dir);
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

	BitVector<TypeOfStuff> typeOfStuffFlags = new BitVector<TypeOfStuff>(TypeOfStuff.class);
	BitVector<Reset> resetFlags = new BitVector<Reset>(Reset.class);

	/**** area loading ****/

	private String             name;
	private String             title;
	private String             author;
	private Map<String, Stuff> stuff = new HashMap<String, Stuff>();
	private Room               recall;

	/** @param file	Filename that the area is read from.
	 @fixme			Throws something, don't just make an empty area. */
	public Area(final File file) throws ParseException, IOException, NamingException {

		/* determine the name by the (file - areaExt) */
		name = file.getName();
		if(!name.endsWith(areaExt)) throw new NamingException(name + " suffix " + areaExt);
		name = name.substring(0, name.length() - areaExt.length());

		/* load the files contents */
		try(
			TextReader in = new TextReader(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
		) {
			/* for exceptions requiring access to 'in' */
			try {
				Scanner scan;
				String recallStr;
				String word, line;
				String id, name = "", title = "", info = "";
				TypeOfStuff what;
				boolean flags[];

				/* grab the header;
				 this.title belongs to Area; title belongs to Stuff in the Area */
				this.title  = in.nextLine();
				this.author = in.nextLine();
				recallStr   = in.nextLine();

				in.assertLine("~");

				/* grab the Stuff */
				while("~".compareTo(line = in.nextLine()) != 0) {
					scan = new Scanner(line);
					if((what = typeOfStuffFlags.find(scan.next())) == null) throw new ParseException("unknown token", in.getLineNumber());
					id = scan.next();
					if(scan.hasNext()) throw new ParseException("too many things", in.getLineNumber());
					
					switch(what) {
						case ROOM:
							Room room = new Room(in);
							stuff.put(id, room);
							name = room.getName();
							title = room.getTitle();
							info = String.format("desc <%s>", room.getDescription());
							break;
						case MOB:
							Mob mob = new Mob(in);
							stuff.put(id, mob);
							name = mob.getName();
							title = mob.getTitle();
							info = String.format("F %b, X %b", mob.isFriendly, mob.isXeno);
							break;
						case OBJECT:
							entities.Object obj = new entities.Object(in);
							stuff.put(id, obj);
							name = obj.getName();
							title = obj.getTitle();
							info = String.format("B %b, T %b", obj.isBreakable, obj.isTransportable);
							break;
						case CHARACTER:
						case CONTAINER:
						case MONEY:
						case PLAYER:
						case STUFF:
							throw new ParseException(what + " not implemented", in.getLineNumber());
					}

					if(FourOneFourMud.isVerbose) System.err.format("%s.%s: name <%s>,  title <%s>, %s.\n", this, id, name, title, info);
				}

				/* set the default room now that we've loaded it (hopefully) */
				if((recall = (Room)stuff.get(recallStr)) == null) throw new ParseException(recallStr + " not found", in.getLineNumber());

				/* resets/connections to the end of the file */
				Stuff thing, target;
				Reset reset;
				Room.Direction dir;
				while((line = in.readLine()) != null) {
					scan = new Scanner(line);
					if((thing  =       stuff.get(scan.next())) == null) throw new ParseException("unknown stuff", in.getLineNumber());
					if((reset  = resetFlags.find(scan.next())) == null) throw new ParseException("unknown token", in.getLineNumber());
					if(reset == Reset.CONNECT || reset == Reset.SET) {
						if((dir = Room.Direction.find(scan.next())) == null) throw new ParseException("unknown direction", in.getLineNumber());
					} else {
						dir = null;
					}
					if((target =  stuff.get(scan.next())) == null) throw new ParseException("unknown argument", in.getLineNumber());
					if(scan.hasNext()) throw new ParseException("too much stuff", in.getLineNumber());
					reset.invoke(thing, target, dir);

					if(FourOneFourMud.isVerbose) System.err.print(this + ": <" + thing + "> <" + reset + "> direction <" + dir + "> to/in <" + target + ">.\n");
				}

			} catch(UnrecognisedTokenException e) {
				/* transform it into ParseException with the line number which
				 we now have */
				throw new ParseException("unrecognised " + e.getMessage(), in.getLineNumber());
			}
		} catch(ParseException e) {
			/* re-throw it: the area is not parseable; harsh, but otherwise
			 [most] area designers would get sloppy */
			throw new ParseException(e.getMessage(), e.getErrorOffset());
		}

		System.err.format("%s: loaded %s, default room %s.\n", file, this, recall);

	}

	/** @return	Default room. */
	public Room getRecall() {
		return recall;
	}

	/** @return	A synecdochical {@link String}. */
	public String toString() {
		return name;
	}

}
