/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt

 Loading/storing of areas.
 
 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.NoSuchElementException;

import java.util.Map;
import java.util.HashMap;
import java.lang.String;

import java.util.Scanner;
import java.util.Collections;

import java.lang.reflect.Method;

import entities.*;
import entities.Object;

class Area {

	private static Map<String, Area> areas = new HashMap<String, Area>();

	/** Load all areas in specified directory.
	 @param strdir
		Where the areas are stored. */
	public static void loadAreas(final String strdir) {
		File dir = new File(strdir);
		if(!dir.exists() || !dir.isDirectory()) {
			System.err.format("loadArea: '%s' is not a thing.\n", strdir);
			return;
		}
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				/* fixme: isFile(), canRead() */
				return name.endsWith(".area");
			}
		});
		for(File f : files) {
			//System.err.print("Loading area <" + f + ">.\n");
			areas.put(f.getName(), new Area(f));
		}
	}

	/** @return areas */
	public static Area getArea(String area) {
		return areas.get(area);
	}

	/* this is a glaring error in Java8; I must pay the price in repeated code */
	/*private interface Flags { static int lookup(final String tok); }*/
	/*public static int lookup(final String tok) { return map.get(tok).ordinal();  }*/

	private enum TypeOfStuff {
		CHARACTER("Character"),
		CONTAINER("Container"),
		MONEY("Money"),
		MOB("Mob"),
		OBJECT("Object"),
		PLAYER("Player"),
		ROOM("Room"),
		STUFF("Stuff");
		private String symbol;
		private static final Map<String, TypeOfStuff> map;
		static {
			Map<String, TypeOfStuff> mod = new HashMap<String, TypeOfStuff>();
			for(TypeOfStuff t : values()) mod.put(t.symbol, t);
			map = Collections.unmodifiableMap(mod);
		}
		private TypeOfStuff(final String symbol)         { this.symbol = symbol; }
		public String toString()                         { return symbol; }
		public static TypeOfStuff find(final String str) { return map.get(str); }
	}

	private enum Reset {
		IN("in"),
		CONNECT("connect"),
		SET("set");
		private String symbol;
		private static final Map<String, Reset> map;
		static {
			Map<String, Reset> mod = new HashMap<String, Reset>();
			for(Reset r : values()) mod.put(r.symbol, r);
			map = Collections.unmodifiableMap(mod);
		}
		private Reset(final String symbol)         { this.symbol = symbol; }
		public String toString()                   { return symbol; }
		public static Reset find(final String str) { return map.get(str); }
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

	private enum MobFlags {
		FRIENDLY("friendly"),
		XENO("xeno");
		private String symbol;
		private static final Map<String, MobFlags> map;
		static {
			/* populate map */
			Map<String, MobFlags> mod = new HashMap<String, MobFlags>();
			for(MobFlags f : values()) mod.put(f.symbol, f);
			map = Collections.unmodifiableMap(mod);
		}
		private MobFlags(final String symbol) { this.symbol = symbol; }
		public static void apply(final String line, boolean bv[]) throws Exception {
			MobFlags sym;
			String toks[] = line.trim().split("\\s++"); /* split on whitespace */
			for(String tok : toks) {
				if((sym = map.get(tok)) == null) throw new Exception("unrecongnised " + tok);
				bv[sym.ordinal()] = true; /* IndexOutOfBounds */
			}
		}
		public static String toLine(boolean bv[]) {
			StringBuilder sb = new StringBuilder();
			boolean isFirst = true;
			for(MobFlags sym : MobFlags.values()) {
				if(bv[sym.ordinal()]) {
					if(isFirst) {
						isFirst = false;
					} else {
						sb.append(" ");
					}
					sb.append(sym);
				}
			}
			return sb.toString();
		}
		public String toString() {
			return symbol;
		}
	}

	private enum ObjectFlags {
		BREAKABLE("breakable"),
		TRANSPORTABLE("transportable");
		private String symbol;
		private static final Map<String, ObjectFlags> map;
		static {
			/* populate map */
			Map<String, ObjectFlags> mod = new HashMap<String, ObjectFlags>();
			for(ObjectFlags f : values()) mod.put(f.symbol, f);
			map = Collections.unmodifiableMap(mod);
		}
		private ObjectFlags(final String symbol) { this.symbol = symbol; }
		public static void apply(final String line, boolean bv[]) throws Exception {
			ObjectFlags sym;
			String toks[] = line.trim().split("\\s++"); /* split on whitespace */
			for(String tok : toks) {
				if((sym = map.get(tok)) == null) throw new Exception("unrecongnised " + tok);
				bv[sym.ordinal()] = true; /* IndexOutOfBounds */
			}
		}
		public static String toLine(boolean bv[]) {
			StringBuilder sb = new StringBuilder();
			boolean isFirst = true;
			for(ObjectFlags sym : ObjectFlags.values()) {
				if(bv[sym.ordinal()]) {
					if(isFirst) {
						isFirst = false;
					} else {
						sb.append(" ");
					}
					sb.append(sym);
				}
			}
			return sb.toString();
		}
		public String toString() {
			return symbol;
		}
	}

	/** LineNumberReader with nextLine, etc. */
	private class MudReader extends LineNumberReader {
		public MudReader(BufferedReader in) {
			super(in);
			setLineNumber(1);
		}
		public String nextLine() throws NoSuchElementException, IOException {
			String line = this.readLine();
			if(line == null) throw new NoSuchElementException("unexpected eof");
			return line;
		}
		public void assertLine(final String asrt) throws IOException, Exception {
			String line = nextLine();
			if(asrt.compareTo(line) != 0) throw new Exception("expected " + asrt);
		}
		public String nextParagraph() throws NoSuchElementException, IOException {
			boolean       isFirst = true;
			StringBuilder sb = new StringBuilder(256);
			String        str;
			for( ; ; ) {
				if((str = nextLine()).length() <= 0) break;
				if(isFirst) {
					isFirst = false;
				} else {
					sb.append(" ");
				}
				sb.append(str);
			}
			return sb.toString();
		}
	}

	private String             title   = "Untitled";
	private String             author  = "Unauthored";
	private Map<String, Stuff> stuff   = new HashMap<String, Stuff>();
	private Room               recall;

	public Area(final File file) {
		String recallStr = null;

		try(
			MudReader in = new MudReader(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
		) {
			Scanner scan;
			String word, line;
			String whatStr, id, name, title;
			TypeOfStuff what;
			boolean flags[];
			in.setLineNumber(1);

			/* grab the header */
			this.title = in.nextLine(); /* this.title belongs to Area; title belongs to Stuff */
			author     = in.nextLine();
			recallStr  = in.nextLine();

			in.assertLine("~");

			/* grab the Stuff */
			while("~".compareTo(line = in.nextLine()) != 0) {
				scan = new Scanner(line);
				if((what = TypeOfStuff.find(scan.next())) == null) throw new Exception("unknown token, line " + in.getLineNumber());
				id = scan.next();
				if(scan.hasNext()) throw new Exception("too many things, line " + in.getLineNumber());
				name  = in.nextLine();
				title = in.nextLine();
				//System.err.format("%s: id:<%s>, name:<%s>, title:<%s>.\n", file, id, name, title);
				switch(what) {
					case ROOM:
						//System.err.format("desc:<%s>.\n", desc);
						stuff.put(id, new Room(name, title, in.nextParagraph()));
						break;
					case MOB:
						flags = new boolean[2];
						line = in.nextLine();
						MobFlags.apply(line, flags);
						//System.err.print(id + ": isF " + flags[0] + "; isX " + flags[1] + "; toLine: <" + MobFlags.toLine(flags) + ">.\n");
						stuff.put(id, new Mob(name, title, flags[0], flags[1]));
						break;
					case OBJECT:
						flags = new boolean[2];
						line = in.nextLine();
						ObjectFlags.apply(line, flags);
						//System.err.print(id + ": isB " + flags[0] + "; isT " + flags[1] + "; toLine: <" + ObjectFlags.toLine(flags) + ">.\n");
						stuff.put(id, new Object(name, title, flags[0], flags[1]));
						break;
					case CHARACTER:
					case CONTAINER:
					case MONEY:
					case PLAYER:
					case STUFF:
						throw new Exception(what + " not implemented, line " + in.getLineNumber());
				}
			}

			/* set the default room now that we've loaded them */
			if(recallStr == null || (recall = (Room)stuff.get(recallStr)) == null) {
				System.err.format("%s: default room %s not found.\n", file, recallStr);
			}

			/* resets to the end */
			Stuff thing, arg;
			Reset reset;
			Room.Direction dir;
			while((line = in.readLine()) != null) {
				scan = new Scanner(line);
				if((thing =  stuff.get(scan.next())) == null) throw new Exception("unknown stuff, line " + in.getLineNumber());
				if((reset = Reset.find(scan.next())) == null) throw new Exception("unknown token, line " + in.getLineNumber());
				if(reset == Reset.CONNECT || reset == Reset.SET) {
					if((dir = Room.Direction.find(scan.next())) == null) throw new Exception("unknown direction, line " + in.getLineNumber());
				} else {
					dir = Room.Direction.N;
				}
				if((arg   =  stuff.get(scan.next())) == null) throw new Exception("unknown argument, line " + in.getLineNumber());
				if(scan.hasNext()) throw new Exception("too much stuff, line " + in.getLineNumber());

				//System.err.print(line + ": <" + thing + ">; <" + reset + ">; <" + dir + ">; <" + arg + ">.\n");
				reset.invoke(thing, arg, dir);
			}

		} catch(NoSuchElementException e) {
			System.err.format("%s: %s.\n", file, e);
		} catch(IOException e) {
			System.err.format("%s: error; %s.\n", file, e);
		} catch(Exception e) {
			System.err.format("%s: error; %s.\n", file, e);
		}

		System.err.format("%s: loaded %s, default room %s.\n", file, this, recall);
	}

	public Room getRecall() {
		return recall;
	}

	public String toString() {
		return "" + title + " by " + author;
	}

}
