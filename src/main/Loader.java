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

/** More abstaction! this is so meta.

 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public abstract class Loader<T> {

	abstract boolean loadNext(TextReader text, Map<String, T> map) throws ParseException, IOException;

	/** Constructor.
	@param ex Something. */
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

		/* go though all the files matching the extension */
		for(File f : files) {

			String name = f.getName();
			name = name.substring(0, name.length() - loadExt.length());

			if(FourOneFourMud.isVerbose) System.err.format("%s: loading <%s>.\n", name, f);

			Map<String, T> mod = new HashMap<String, T>();

			try(
				TextReader in = new TextReader(Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8));
			) {
				while(loadNext(in, mod));
			} catch(ParseException e) {
				System.err.format("%s; syntax error: %s, line %d.\n", f, e.getMessage(), e.getErrorOffset());
			} catch(IOException e) {
				System.err.format("%s; %s.\n", f, e);
			}

			loadMod.put(name, Collections.unmodifiableMap(mod));
		}

		return Collections.unmodifiableMap(loadMod);

	}

	/** Javadocs {@link URL}.
	 <p>
	 More.
	 
	 @param p			Thing.
	 @return			Thing.
	 @throws Exception	Thing.
	 @see				package.Class#method(Type)
	 @see				#field
	 @since				1.0
	 @deprecated		Ohnoz. */

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Hi";
	}

}
