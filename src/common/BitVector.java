/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

//import java.io.LineNumberReader;

import java.util.Map;
import java.util.HashMap;

import java.util.Collections;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import common.UnrecognisedTokenException;

/** Meta-binary/bitvector flags.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class BitVector<E extends Enum<E>> {

	private final Field    aField;
	private final Class<E> aClass;

	private String name;

	private boolean flags[];
	private Map<String, E> map = null;
	//private Enum<E> e;

	/** Contstuct a BitVector out of a {@link Class}.
	 @param aClass					An Enum having a unique varible "symbol."
	 @throws NoSuchFieldException	When the aClass is not Enum or doesn't have
	 the field symbol. */
	public BitVector(final Class<E> aClass) {

		if(!aClass.isEnum()) {
			assert(true): "BitVector called not on enum";
			System.err.format("%s: inconceivable! BitVector must be an enum.\n", this);
		}

		Field aField = null;
		try {
			aField = aClass.getDeclaredField("symbol");
		} catch(NoSuchFieldException e) {
			assert(true): "BitVector must have <public symbol>; " + e;
			System.err.format("%s: inconceivable! %s.\n", this, e);
		}
		this.aField = aField;
		this.aClass = aClass;

		name = aClass.getName();

		flags = new boolean[aClass.getEnumConstants().length];
		//for(E val : aClass.getEnumConstants()) System.err.format("%s : %s\n", val, aField.get(val));

		/* populate a map from aField strings to enum things; make it fast */
		try {
			Map<String, E> mod = new HashMap<String, E>();
			for(E val : aClass.getEnumConstants()) mod.put((String)aField.get(val), val);
			map = Collections.unmodifiableMap(mod);
		} catch(IllegalAccessException e) {
			assert(true): "inconceivable! " + e;
			System.err.format("%s: inconceivable! %s.\n", this, e);
		}
	}

	/** Find.
	 @param find	String.
	 @return		Enum value (can be null.) */
	public E find(final String f) {
		return map.get(f);
	}

	/** Reads the line and sets boolean array appropriately.
	 @param line			The line with only the strings from the enum.
	 @return				The values arranged by incresing order.
	 @throws ParseException	On any tokens not in the enum. */
	public boolean[] fromLine(final String line) throws UnrecognisedTokenException {
		E sym;
		/* split on whitespace */
		String toks[] = line.trim().split("\\s++");
		/* clear */
		for(int i = 0; i < flags.length; i++) flags[i] = false;
		/* set */
		for(String tok : toks) {
			if((sym = map.get(tok)) == null) throw new UnrecognisedTokenException(tok);
			flags[sym.ordinal()] = true;
		}
		return flags;
	}

	/**
	 @param bv	Must have size() elements (at least -- the values above size()
	 are superflous.)
	 @return	A string with the representations of the true bit values set. */
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
					assert(true): "inconceivable! " + e;
					System.err.format("%s: inconceivable! %s.\n", this, e);
				}
			}
		}
		return sb.toString();
	}

	/*public Enum<E> getEnum() {
		return e;
	}*/

	/** Eg, { boolean b[] = new boolean[f.size()]; }.
	 @return	The number of constants. */
	public int size() {
		return aClass.getEnumConstants().length;
	}

	/** @return	A synecdochical {@link String}. */
	public String toString() {
		return "BitVector(" + name + ")";
	}

}
