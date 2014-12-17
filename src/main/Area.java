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

import entities.Stuff;

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
				/* isFile(), canRead() */
				return name.endsWith(".area");
			}
		});
		for(File f : files) {
			System.err.print("> " + f + "\n");
			areas.put(f.getName(), new Area(f));
		}
	}

	/* stuff enum */
	private enum TypeOfStuff {
		CHARACTER("Character"),
		CONTAINER("Container"),
		MONEY("Money"),
		NPC("NPC"),
		OBJECT("Object"),
		PLAYER("Player"),
		ROOM("Room"),
		STUFF("Stuff");
		private String symbol;
		private static final Map<String, TypeOfStuff> back = new HashMap<String, TypeOfStuff>();
		static {
			back.put("Ch", CHARACTER);
			back.put("Room", ROOM);
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

	private String             title   = "Untitled";
	private String             author  = "Unauthored";
	private Map<String, Stuff> stuff;
	private Stuff              recall;

	public Area(final File file) {
		int no = 0;

		try(
			//BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
			//Scanner in = new Scanner(file.toPath(), StandardCharsets.UTF_8);
			Scanner in = new Scanner(file);
		) {
			String line, word[], recallStr;
			String whatStr, id, name, title;
			String temp, desc;
			TypeOfStuff what;
			boolean isFirst;

			/* grab the header */
			no++;
			title     = in.nextLine();
			no++;
			author    = in.nextLine();
			no++;
			recallStr = in.nextLine();

			/* grab the rest */
			while((line = in.nextLine()) != null) {
				no++;
				if(line.compareTo("~") != 0) throw new Exception("expecting ~");
				no++;
				whatStr = in.next();
				if((what = TypeOfStuff.find(whatStr)) == null) throw new Exception("unknown " + whatStr);
				id = in.next();
				if(in.nextLine().length() != 0) throw new Exception("too many things");
				no++;
				name = in.nextLine();
				no++;
				title = in.nextLine();
				System.err.format("%s: id:<%s>, name:<%s>, title<%s>.\n", file, id, name, title);
				switch(what) {
					case ROOM:
						isFirst = true;
						desc = "";
						for( ; ; ) {
							no++;
							if((temp = in.nextLine()).length() <= 0) break;
							desc += (isFirst ? "" : " ") + temp; /* fixme: sb */
							isFirst = false;
						}
						System.err.format("desc:<%s>.\n", desc);
						break;
					case CHARACTER:
					case CONTAINER:
					case MONEY:
					case NPC:
					case OBJECT:
					case PLAYER:
					case STUFF:
						throw new Exception(what + " not implemented");
				}
			}

			/* set the default room */
			//recall = stuff.get(rStr);

		} catch(NoSuchElementException e) {
			System.err.format("Area: syntax error in %s at line %d; %s.\n", file, no, e);
		} catch(IOException e) {
			System.err.format("Area: error in %s; %s.\n", file, e);
		} catch(Exception e) {
			System.err.format("Area: error in %s at line %d; %s.\n", file, no, e);
		}
		System.err.format("Area: %s.\n", this);
	}

	public String toString() {
		return "Area " + title + "; " + author;
	}

}
