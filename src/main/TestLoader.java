/** Copyright 20xx Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Map;
import common.TextReader;
import java.util.Scanner;

import java.text.ParseException;
import java.io.IOException;

/** Java.
 <p>
 More {@link url}.
 
 @author	Neil
 @version	1.0, 01-20xx
 @since		1.0, 01-20xx */
class TestLoader extends Loader<String> {

	private Map<String, Map<String, String>> map;
	private static final String csetDir = "data/commandsets";
	private static final String csetExt = ".cset";

	@Override
	boolean loadNext(TextReader in, Map<String, String> map, int foo[]) throws ParseException, IOException {
		String line, alias, cmdStr;

		if((line = in.readLine()) == null) return false;

		Scanner scan = new Scanner(line);

		if((alias  = scan.next()) == null) throw new ParseException("alias",   in.getLineNumber());
		if((cmdStr = scan.next()) == null) throw new ParseException("command", in.getLineNumber());
		if(scan.hasNext())                 throw new ParseException("too much stuff", in.getLineNumber());
		map.put(alias, cmdStr + foo[0]++);
		/*try {
			command = (Command)Commandset.class.getDeclaredField(cmdStr).get(null);
			if(FourOneFourMud.isVerbose) System.err.format("%s: command <%s>: \"%s\"->%s\n", name, alias, cmdStr, command);
			mod.put(alias, command);
		} catch(NoSuchFieldException | IllegalAccessException e) {
			System.err.format("%s (line %d:) no such command? %s.\n", f, in.getLineNumber(), e);
		}*/

		return true;
	}

	/** Constructor.
	@param ex Something. */
	public TestLoader() throws IOException {
		map = load(csetDir, csetExt);
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
		String a = "----\n";
		for(Map.Entry<String, Map<String, String>> e : map.entrySet()) {
			a += "! " + e.getKey() + "\n";
			for(Map.Entry<String, String> f : e.getValue().entrySet()) {
				a += "\t(" + f.getKey() + " -> " + f.getValue() + ")\n";
			}
		}
		return a;
	}

}
