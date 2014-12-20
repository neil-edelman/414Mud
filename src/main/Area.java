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

import common.TextReader;
import common.BitVector;
import common.ParseException;
import entities.*;

/** Loading/storing of areas. 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
class Area {

	private static final String areaExt = ".area";

	private static Map<String, Area> areas = new HashMap<String, Area>();

	/** Load all areas in specified directory.
	 @param strdir	Where the areas are stored. */
	public static void loadAreas(final String strdir) {
		File dir = new File(strdir);
		if(!dir.exists() || !dir.isDirectory()) {
			System.err.format("loadArea: '%s' is not a thing.\n", strdir);
			return;
		}
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				/* fixme: isFile(), canRead() */
				return name.endsWith(areaExt);
			}
		});
		for(File f : files) {
			System.err.print("Loading area <" + f + ">.\n");
			areas.put(f.getName(), new Area(f));
		}
	}

	/**
	 @param area	String name (same as filename without .area.)
	 @return		Area or null. */
	public static Area getArea(String area) {
		return areas.get(area);
	}

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

	BitVector<TypeOfStuff> typeOfStuffFlags = new BitVector<TypeOfStuff>(TypeOfStuff.class);

	public enum Reset {
		IN("in"),
		CONNECT("connect"),
		SET("set");
		public String symbol;
		private Reset(final String symbol)         { this.symbol = symbol; }
		public String toString()                   { return symbol; }
		public void invoke(final Stuff thing, final Stuff arg, final Room.Direction dir) throws Exception {
			//System.err.format("%s.invoke(%s, %s, %s);\n", this, thing, arg, dir);
			Reset.class.getDeclaredMethod(symbol, Stuff.class, Stuff.class,
										  Room.Direction.class).invoke(this, thing, arg, dir);
		}
		public void in(final Stuff thing, final Stuff container, final Room.Direction dir) {
			//System.out.format("!!!refl in in %s %s %s\n", thing, container, dir);
			thing.transportTo(container);
		}
		public void connect(final Stuff room, final Stuff target, final Room.Direction dir) throws Exception {
			//System.out.format("!!!refl connect in %s %s %s\n", room, target, dir);
			//if(!(room   instanceof Room)) throw new Exception(room   + " not Room");
			//if(!(target instanceof Room)) throw new Exception(target + " not Room");
			Room r = (Room)room;
			r.connectDirection(dir, (Room)target);
		}
		public void set(final Stuff room, final Stuff target, final Room.Direction dir) throws Exception {
			//System.out.format("!!!refl set in %s %s %s\n", room, target, dir);
			Room r = (Room)room;
			r.setDirection(dir, (Room)target);
		}
	}

	BitVector<Reset> resetFlags = new BitVector<Reset>(Reset.class);

	public enum MobFlags {
		FRIENDLY("friendly"),
		XENO("xeno");
		public String symbol;
		private MobFlags(final String symbol) { this.symbol = symbol; }
		public String toString()              { return symbol; }
	}

	BitVector<MobFlags> mobFlags = new BitVector<MobFlags>(MobFlags.class);

	public enum ObjectFlags {
		BREAKABLE("breakable"),
		TRANSPORTABLE("transportable");
		public String symbol;
		private ObjectFlags(final String symbol) { this.symbol = symbol; }
		public String toString()                 { return symbol; }
	}

	BitVector<ObjectFlags> objectFlags = new BitVector<ObjectFlags>(ObjectFlags.class);

	public enum Things {
		ABC("abc"),
		DEF("def"),
		GHI("ghi");
		public String symbol;
		private Things(final String symbol) { this.symbol = symbol; }
	}

	BitVector<Things> thingsFlags = new BitVector<Things>(Things.class);

	private String             name;
	private String             title   = "Untitled";
	private String             author  = "Unauthored";
	private Map<String, Stuff> stuff   = new HashMap<String, Stuff>();
	private Room               recall;

	/** @param file	Filename that the area is read from.
	 @fixme			Throws something, don't just make an empty area. */
	public Area(final File file) {
		String recallStr = null;

		/* determine the name by the (file - areaExt) */
		name = file.getName();
		if(name.endsWith(areaExt)) {
			name = name.substring(0, name.length() - areaExt.length());
		} else {
			System.err.format("%s: area file didn't end in %s.\n", name, areaExt);
		}

		/* load the files contents */
		try(
			TextReader in = new TextReader(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
		) {
			Scanner scan;
			String word, line;
			String id, name, title, desc, info = "no info";
			TypeOfStuff what;
			boolean flags[];

			/* grab the header;
			 this.title belongs to Area; title belongs to Stuff in the Area */
			this.title = in.nextLine();
			author     = in.nextLine();
			recallStr  = in.nextLine();

			in.assertLine("~");
			System.err.print(" A \n");

			/* grab the Stuff */
			while("~".compareTo(line = in.nextLine()) != 0) {
				scan = new Scanner(line);
				/* Exception -> FileParseException */
				if((what = typeOfStuffFlags.find(scan.next())) == null) throw new ParseException(in, "unknown token");
				id = scan.next();
				if(scan.hasNext()) throw new ParseException(in, "too many things");
				name  = in.nextLine();
				title = in.nextLine();
				switch(what) {
					case ROOM:
						stuff.put(id, new Room(name, title, desc = in.nextParagraph()));
						info = String.format("desc <%s>", desc);
						break;
					case MOB:
						flags = mobFlags.fromLine(in.nextLine());
						stuff.put(id, new Mob(name, title, flags[0], flags[1]));
						info = String.format("F %b, X %b", flags[0], flags[1]);
						break;
					case OBJECT:
						flags = objectFlags.fromLine(in.nextLine());
						stuff.put(id, new entities.Object(name, title, flags[0], flags[1]));
						info = String.format("B %b, T %b", flags[0], flags[1]);
						break;
					case CHARACTER:
					case CONTAINER:
					case MONEY:
					case PLAYER:
					case STUFF:
						throw new ParseException(in, what + " not implemented");
				}

				if(FourOneFourMud.isVerbose) System.err.format("%s.%s: name <%s>,  title <%s>, %s.\n", this, id, name, title, info);
			}
			System.err.print(" B \n");

			/* set the default room now that we've loaded them */
			if(recallStr == null || (recall = (Room)stuff.get(recallStr)) == null) {
				System.err.format("%s: default room %s not found.\n", file, recallStr);
			}

			/* resets to the end */
			Stuff thing, target;
			Reset reset;
			Room.Direction dir;
			while((line = in.readLine()) != null) {
				scan = new Scanner(line);
				if((thing  =       stuff.get(scan.next())) == null) throw new ParseException(in, "unknown stuff");
				if((reset  = resetFlags.find(scan.next())) == null) throw new ParseException(in, "unknown token");
				if(reset == Reset.CONNECT || reset == Reset.SET) {
					if((dir = Room.Direction.find(scan.next())) == null) throw new ParseException(in, "unknown direction");
				} else {
					dir = null;
				}
				if((target =  stuff.get(scan.next())) == null) throw new ParseException(in, "unknown argument");
				if(scan.hasNext()) throw new ParseException(in, "too much stuff");
				reset.invoke(thing, target, dir);

				if(FourOneFourMud.isVerbose) System.err.print(this + ": <" + thing + "> <" + reset + "> direction <" + dir + "> to/in <" + target + ">.\n");
			}
			System.err.print(" C \n");

		} catch(ParseException e) {
			System.err.format(" *** %s (syntax error:) %s.\n", file, e.getMessage());
		} catch(Exception e) {
			/* fixme: have nested, re-interrupt */
			System.err.format(" *** %s: %s.\n", file, e);
		}

		System.err.format("%s: loaded %s, default room %s.\n", file, this, recall);

		System.err.print("EXPERIMENT\n");
		try {
			BitVector<Things> obj = new BitVector<Things>(Things.class);
			boolean a[] = new boolean[obj.size()];
			a[1] = true;
			a[2] = true;
			System.err.format("%b %b %b : %s %d\n", a[0], a[1], a[2], obj.toLine(a), obj.size());
			String str = new String("abc def");
			boolean z[] = obj.fromLine(str);
			System.err.format("<%s> : %b %b %b\n", str, z[0], z[1], z[2]);
		} catch (NoSuchFieldException e) {
			System.err.format("%s: enum has to have variable <%s>.\n", this, e.getMessage());
		} catch (Exception e) {
			System.err.format(">>> %s\n", e);
		}
		System.err.print("EXPERIMENT DONE\n");
	}

	/** @return	Default room. */
	public Room getRecall() {
		return recall;
	}

	/** @return	A synecdochical {@link String}. */
	public String toString() {
		//return "" + title + " by " + author;
		return name;
	}

}
