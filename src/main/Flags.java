/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt

 Meta-binary/bitvector flags.

 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.util.Map;
import java.util.HashMap;

import java.util.Collections;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class Flags<E extends Enum<E>> {

	private /*final <- there is no way to assign a final in a try*/ Field    aField;
	private final Class<E> aClass;

	private String name;

	private boolean        flags[];
	private Map<String, E> map = null;
	//private Enum<E> e;

	/** contstuct a Flags out of a String in an enum.
	 @param aClass
		An Enum having the varible "symbol" that is unique. */
	public Flags(final Class<E> aClass) {

		System.err.print("start\n");
		if(!aClass.isEnum()) { //throw new Exception("flags only works on enums");
			System.err.print("Flags only work on enums.\n");
		}

		try {
			this.aField = aClass.getDeclaredField("symbol");
		} catch(NoSuchFieldException e) {
			this.aField = null;
			System.err.format("%n.\n", e);
		}
		this.aClass = aClass;

		name = aClass.getName();

		flags = new boolean[aClass.getEnumConstants().length];
		//for(E val : aClass.getEnumConstants()) System.err.format("%s : %s\n", val, aField.get(val));

		System.err.print("mid\n");
		/* populate a map from aField strings to enum things; make it fast */
		try {
			Map<String, E> mod = new HashMap<String, E>();
			for(E val : aClass.getEnumConstants()) mod.put((String)aField.get(val), val);
			map = Collections.unmodifiableMap(mod);
		} catch(IllegalAccessException e) {
			System.err.format("%s: inconceivable! %s.\n", this, e);
		}
		System.err.print("end\n");
	}

	/** @param find
		String.
	 @return
		Enum value (can be null.) */
	public E find(final String f) {
		return map.get(f);
	}

	/** @param line
		The line with only the strings from the enum.
	 @return
		The values arranged by incresing order.
	 @throws Exception
		On any tokens not in the enum. */
	public boolean[] fromLine(final String line) throws Exception {
		E sym;
		/* split on whitespace */
		String toks[] = line.trim().split("\\s++");
		/* clear */
		for(int i = 0; i < flags.length; i++) flags[i] = false;
		/* set */
		for(String tok : toks) {
			if((sym = map.get(tok)) == null) throw new Exception("unrecongnised " + tok); // fixme
			flags[sym.ordinal()] = true; /* IndexOutOfBounds */
		}
		return flags;
	}

	/** Returns a string with the representations of the true bit values
	 set.
	 @param bv
		Must have size() elements (at least -- the values above size() are
		superflous.) */
	public String toLine(boolean bv[]) {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for(E val : aClass.getEnumConstants()) {
			if(bv[val.ordinal()]) {
				if(isFirst) {
					isFirst = false;
				} else {
					sb.append(" ");
				}
				try {
					sb.append(aField.get(val));
				} catch(IllegalAccessException e) {
					System.err.format("%s: inconceivable! %s.\n", this, e);
				}
			}
		}
		return sb.toString();
	}

	/*public Enum<E> getEnum() {
		return e;
	}*/

	public String toString() {
		return "Flags(" + name + ")";
	}

	public int size() {
		return aClass.getEnumConstants().length;
	}

}
