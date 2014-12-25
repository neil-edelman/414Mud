/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.io.File;
import java.nio.file.Files;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.nio.charset.StandardCharsets;
import common.TextReader;
import java.io.IOException;

import main.FourOneFourMud;

/** More abstaction! This is kind of messy, not used.

 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public abstract class Loader<T> {

	/** This reads an entry then puts in in the map supplied.
	 @param in		The open TextReader.
	 @param map		The map.
	 @param part	A one element list that the initial value of [0]; loadNext
	 does not change this value until the end of a file, then it resets it.
	 @return		True if you are not done, false otherwise. Typically,
	 < if(in.readLine() == null) return false; >. */
	abstract boolean loadNext(TextReader in, Map<String, T> map, int part[]) throws ParseException, IOException;

	/** Loads all stuff from a directory.
	 @param loadDir			Which directory to load.
	 @param loadExt			Which file types to load based on their extension, like
	 ".txt."
	 @throws IOException	If the directory was not found. */
	Map<String, Map<String, T>> load(final String loadDir, final String loadExt) throws IOException {

		/* directory */
		File dir = new File(loadDir);
		if(!dir.exists() || !dir.isDirectory()) throw new IOException("<" + loadDir + "> not found");

		/* get the files */
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return name.endsWith(loadExt);
			}
		});

		Map<String, Map<String, T>> loadMod = new HashMap<String, Map<String, T>>();

		String name;
		int part[] = new int[1];

		/* go though all the files matching the extension */
		for(File f : files) {

			name    = f.getName();
			name    = name.substring(0, name.length() - loadExt.length());
			part[0] = 0;

			Map<String, T> mod = new HashMap<String, T>();

			try(
				TextReader in = new TextReader(Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8));
			) {
				while(loadNext(in, mod, part));
			} catch(ParseException e) {
				System.err.format("%s; syntax error: %s, line %d.\n", f, e.getMessage(), e.getErrorOffset());
				continue;
			} catch(IOException e) {
				System.err.format("%s: %s.\n", f, e);
				continue;
			}

			loadMod.put(name, Collections.unmodifiableMap(mod));
			if(FourOneFourMud.isVerbose) System.err.format("%s: loaded <%s>.\n", name, f);
		}

		return Collections.unmodifiableMap(loadMod);

	}

}
