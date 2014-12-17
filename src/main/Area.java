package main;

import java.io.BufferedReader;
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

import entities.*;
import entities.Object;

/* Areas. */

class Area {

	static Map<String, Area> areas = new HashMap<String, Area>();

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

	private enum TypeOfStuff {
		CHARACTER("Character"),
		CONTAINER("Container"),
		MONEY("Money"),
		MOB("Mob"),
		OBJECT("Object"),
		PLAYER("Player"),
		ROOM("Room"),
		STUFF("Stuff"),
		PUT("*");
		private String symbol;
		private static final Map<String, TypeOfStuff> back = new HashMap<String, TypeOfStuff>();
		static {
			back.put("*", PUT);
			back.put("Ch", CHARACTER);
			back.put("Room", ROOM);
			back.put("Mob", MOB);
			back.put("Object", OBJECT);
			/* etc; fixme: find out how to get this in constuctor */
		}
		private TypeOfStuff(final String symbol) {
			this.symbol = symbol;
			//back.put(symbol, this);
		}
		public static TypeOfStuff find(final String str) {
			return back.get(str);
		}
		public String toString() {
			return symbol;
		}
	}

	/* these are weak */

	private enum MobFlags {
		FRIENDLY("friendly"),
		XENO("xeno");
		private String symbol;
		private static final Map<String, MobFlags> back = new HashMap<String, MobFlags>();
		static {
			back.put("friendly", FRIENDLY);
			back.put("xeno", XENO);
			/* etc; fixme: find out how to get this in constuctor */
		}
		private MobFlags(final String symbol) {
			this.symbol = symbol;
			//back.put(symbol, this);
		}
		public static void set(final String str, Boolean isFriendly, Boolean isXeno) throws Exception {
			MobFlags f;
			if((f = back.get(str)) == null) throw new Exception("unrecongnised " + str);
			switch(f) {
				case FRIENDLY:
					isFriendly = true;
					System.err.print("in MobFlags isF "+isFriendly+"\n");
					break;
				case XENO:
					isXeno = true;
					break;
			}
		}
		public String toString() {
			return symbol;
		}
	}

	private enum ObjectFlags {
		BREAKABLE("breakable"),
		TRANSPORTABLE("transportable");
		private String symbol;
		private static final Map<String, ObjectFlags> back = new HashMap<String, ObjectFlags>();
		static {
			back.put("breakable", BREAKABLE);
			back.put("transportable", TRANSPORTABLE);
			/* etc; fixme: find out how to get this in constuctor */
		}
		private ObjectFlags(final String symbol) {
			this.symbol = symbol;
			//back.put(symbol, this);
		}
		public static void set(final String str, Boolean isBreakable, Boolean isTransportable) throws Exception {
			ObjectFlags f;
			if((f = back.get(str)) == null) throw new Exception("unrecongnised " + str);
			switch(f) {
				case BREAKABLE:
					isBreakable = true;
					break;
				case TRANSPORTABLE:
					isTransportable = true;
					break;
			}
		}
		public String toString() {
			return symbol;
		}
	}

	private String             title   = "Untitled";
	private String             author  = "Unauthored";
	private Map<String, Stuff> stuff   = new HashMap<String, Stuff>();
	private Stuff              recall;

	public Area(final File file) {
		String recallStr = null;
		int no = 0;

		try(
			/* fixme: yes, do this: */
			//BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
			Scanner in = new Scanner(file);
		) {
			Scanner scanLine;
			String word, line;
			String whatStr, id, name, title;
			TypeOfStuff what;

			/* grab the header */
			no++;
			this.title = in.nextLine(); /* this.title belongs to Area; title belongs to Stuff */
			no++;
			author     = in.nextLine();
			no++;
			recallStr  = in.nextLine();

			/* grab the rest */
			while(in.hasNextLine()) {
				no++;
				line = in.nextLine();
				if(line.compareTo("~") != 0) throw new Exception("expecting ~");
				no++;
				if((line = in.nextLine()) == null) throw new Exception("unexpected eof");
				//System.err.format("%d: <%s>\n", no, line);
				scanLine = new Scanner(line);
				whatStr = scanLine.next();
				if((what = TypeOfStuff.find(whatStr)) == null) throw new Exception("unknown " + whatStr);

				/* special case! fixme: ugly */
				if(what == TypeOfStuff.PUT) {
					String str, dir;
					Stuff obj, target;

					str = scanLine.next();
					obj = stuff.get(str);
					if(obj == null) throw new Exception(str + " not found");
					str = scanLine.next();
					if("in".compareTo(str) == 0) {
						str = scanLine.next();
						target = stuff.get(str);
						if(target == null) throw new Exception(str + " not found");
						//System.err.print(obj + " in " + target + "\n");
						obj.transportTo(target);
					} else if("connect".compareTo(str) == 0) {
						dir = scanLine.next();
						str = scanLine.next();
						target = stuff.get(str);
						if(target == null) throw new Exception(str + " not found");
						System.err.print(obj + " connect " + dir + " to " + target + "\n");
						//obj.connectDirection();
					} else if("set".compareTo(str) == 0) {
						dir = scanLine.next();
						str = scanLine.next();
						target = stuff.get(str);
						if(target == null) throw new Exception(str + " not found");
						System.err.print(obj + " set " + dir + " to " + target + "\n");
					} else {
						throw new Exception("what " + str);
					}
					if(scanLine.hasNext()) throw new Exception("too many things");
					continue;
				}

				/* . . . continuing */
				id = scanLine.next();
				if(scanLine.hasNext()) throw new Exception("too many things");
				no++;
				name = in.nextLine();
				no++;
				title = in.nextLine();
				//System.err.format("%s: id:<%s>, name:<%s>, title:<%s>.\n", file, id, name, title);
				switch(what) {
					case ROOM:
						boolean isFirst = true;
						String desc = "", temp;
						for( ; ; ) {
							no++;
							if((temp = in.nextLine()).length() <= 0) break;
							desc += (isFirst ? "" : " ") + temp; /* fixme: sb */
							isFirst = false;
						}
						//System.err.format("desc:<%s>.\n", desc);
						stuff.put(id, new Room(name, title, desc));
						break;
					case MOB:
						no++;
						scanLine = new Scanner(in.nextLine());
						boolean isF = false, isX = false;
						while(scanLine.hasNext()) {
							word = scanLine.next();
							try {
								MobFlags.set(word, isF, isX);
								System.err.print("in Area: isF " + isF + "\n");
							} catch(Exception e) {
								System.err.format("%s: line %d; %s.\n", file, no, e);
							}
						}
						System.err.print("isF " + isF + " isX " + isX + "\n");
						stuff.put(id, new Mob(name, title, isF, isX));
						break;
					case OBJECT:
						no++;
						scanLine = new Scanner(in.nextLine());
						boolean isBreakable = false, isTransportable = false;
						while(scanLine.hasNext()) {
							word = scanLine.next();
							try {
								ObjectFlags.set(word, isBreakable, isTransportable);
							} catch(Exception e) {
								System.err.format("%s: line %d; %s.\n", file, no, e);
							}
						}
						stuff.put(id, new Object(name, title, isBreakable, isTransportable));
						break;
					case CHARACTER:
					case CONTAINER:
					case MONEY:
					case PLAYER:
					case STUFF:
						throw new Exception(what + " not implemented");
				}
			}

		} catch(NoSuchElementException e) {
			System.err.format("%s: syntax error at line %d; %s.\n", file, no, e);
		} catch(IOException e) {
			System.err.format("%s: error; %s.\n", file, e);
		} catch(Exception e) {
			System.err.format("%s: error at line %d; %s.\n", file, no, e);
		}
		/* set the default room now that we've loaded them */
		if(recallStr == null || (recall = stuff.get(recallStr)) == null) {
			System.err.format("%s: default room %s not found.\n", file, recallStr);
		}
		System.err.format("%s: loaded %s, default room %s.\n", file, this, recall);
	}

	public String toString() {
		return "" + title + " by " + author;
	}

}
