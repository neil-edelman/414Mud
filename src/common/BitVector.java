/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

//import java.io.LineNumberReader;

import java.util.Map;
import java.util.HashMap;

import java.util.Collections;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import java.lang.RuntimeException;

import common.UnrecognisedTokenException;

import entities.Stuff;

/** Meta-binary/bitvector flags.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.1, 12-2014 */
public class BitVector<E extends Enum<E>> {

	private static final String symbol = "symbol";

	// FIXME: don't need all of these fields

	private final Class<E> aClass;
	private final Field    aField;

	private boolean flags[];
	private Map<String, E> map = null;
	//private Enum<E> e;

	private Field vector[];

	/** Contstuct a BitVector out of a {@link Class}.
	 @param aClass					An Enum having a unique varible "symbol."
	 @throws NoSuchFieldException	When the aClass is not Enum or doesn't have
	 the field symbol. */
	public BitVector(final Class<E> aClass) {

		if(!aClass.isEnum()) throw new RuntimeException("" + aClass + " not an enum");

		E   set[]  = aClass.getEnumConstants();
		int length = set.length;

		try {

			/* fill in the BitVector; flags is pre-allocate for returning */
			this.aClass = aClass;
			this.aField = aClass.getDeclaredField(symbol);
			this.flags  = new boolean[length];

			/* refected values */
			vector = new Field[length];
			int i  = 0;
			for(E thing : set) vector[i++] = aClass.getDeclaredField(thing.name());

			/* a mapping of Strings */
			Map<String, E> mod = new HashMap<String, E>();
			for(E val : set) mod.put((String)aField.get(val), val);
			map = Collections.unmodifiableMap(mod);

		} catch(NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("inconceivable! " + e);
		}

	}

	/** Find.
	 @param find	String.
	 @return		Enum value (can be null.) */
	public E find(final String f) {
		return map.get(f);
	}

	public void fromLine(final Stuff stuff, final String line) throws UnrecognisedTokenException {
		//fixme: if( extends Stuff)
		E sym;
		/* split on whitespace */
		String toks[] = line.trim().split("\\s++");
		/* clear */
		for(int i = 0; i < flags.length; i++) flags[i] = false;
		/* set */
		for(String tok : toks) {
			if((sym = map.get(tok)) == null) throw new UnrecognisedTokenException(tok);
			flags[sym.ordinal()] = true;
			try {
				/* something -> isSomething = true */
				String var = sym.toString();
				var = "is" + var.substring(0, 1).toUpperCase() + var.substring(1);
				stuff.getClass().getField(var).setBoolean(stuff, true);
				//System.err.format("> %s true! %s\n", var, stuff.getClass().getField(var));
			} catch(NoSuchFieldException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		/* set fields */
//		for(Field field : stuff.getFields()) {
//			System.err.format("> <%s> <%s> %b\n", field, field.getGenericType(), field.getGenericType() == boolean.class);
//			if(field.getGenericType() != boolean.class) continue;
			/* it's a public boolean variable, assume */
//		}
	}

	/** Reads the line and sets boolean array appropriately.
	 @param line			The line with only the strings from the enum.
	 @return				The values arranged by incresing order.
	 @throws ParseException	On any tokens not in the enum. */
//	public boolean[] fromLine(final String line) throws UnrecognisedTokenException {
//		E sym;
//		/* split on whitespace */
//		String toks[] = line.trim().split("\\s++");
//		/* clear */
//		for(int i = 0; i < flags.length; i++) flags[i] = false;
//		/* set */
//		for(String tok : toks) {
//			if((sym = map.get(tok)) == null) throw new UnrecognisedTokenException(tok);
//			flags[sym.ordinal()] = true;
//		}
//		return flags;
//	}

	/**
	 @param bv	Must have size() elements
	 @return	A string with the representations of the true bit values set. */
	public String toLine(boolean bv[]) throws Exception {
		if(bv.length != vector.length) throw new Exception("" + bv + " has to have " + vector.length + " elements");
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
		return "BitVector(" + aClass.getName() + ")";
	}

}
