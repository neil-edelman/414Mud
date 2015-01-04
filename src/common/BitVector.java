/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package common;

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

	/* fixme: right now, it doesn't specify anything: trust that the
	 public String symbol is defined */
	public interface Flags {
		/*public String getSymbol;*/
	}

	private static final String symbol = "symbol";

	private final Class<E> aClass;
	private final Field    aField;

	private final Map<String, E> map;

	/* buffer to return from {@link fromLine} */
	private boolean flags[];

	/** Contstuct a BitVector out of a {@link Class}.
	 @param aClass					An Enum having a unique varible "symbol."
	 @throws NoSuchFieldException	When the aClass is not Enum or doesn't have
	 the field symbol. */
	public BitVector(final Class<E> aClass) {

		if(!aClass.isEnum()) throw new RuntimeException("" + aClass + " not an enum");

		E enumc[] = aClass.getEnumConstants();

		try {

			/* fill in the BitVector; flags is pre-allocate for returning */
			this.aClass = aClass;
			this.aField = aClass.getDeclaredField(symbol);
			this.flags  = new boolean[enumc.length];

			/* a mapping of Strings to E so we can get better then O(n) */
			Map<String, E> mod = new HashMap<String, E>();
			for(E val : enumc) mod.put((String)aField.get(val), val);
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

	/** Auto-magically sets public fields, "foo" -> isFoo = true.
	 @param stuff	The {@link entities.Stuff} you want to be modified.
	 @param line	The line of how you want it to be modifed. */
	public void fromLine(final Stuff stuff, final String line) throws UnrecognisedTokenException {
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
	}

	/**
	 @param bv	Must have size() elements
	 @return	A string with the representations of the true bit values set. */
	public String toLine(boolean bv[]) throws Exception {
		if(bv.length != map.size()) throw new Exception("" + bv + " has to have " + map.size() + " elements");
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
