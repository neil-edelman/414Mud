/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.Map;
//import java.util.HashMap;
//import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import java.util.NoSuchElementException;
import java.lang.UnsupportedOperationException;
import java.lang.IllegalStateException;

import common.Chance;
import main.Mapper;
import entities.Mob;
import entities.Stuff;

/** Chronos is the main AI thing; it's run is called every second. It is a
 subclass of abstract Handler which is a subclass of Runnable. Each thread has
 a Runnable, so this is [probably] a thread.
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.1, 2014-12 */
public class Chronos implements Mud.Handler {

	private static final String stuffCommandset = "stuff";

	private final Chance     chance = new Chance();
	private final Mapper     mapper = new Mapper();
	private      List<Stuff> active = new LinkedList<Stuff>(); /* <- hashmap!!? */
	private final Mud           mud;
	private	final Map<String, Command> commands; /* could be null! */

	/** Constructor. */
	public Chronos(final Mud mud) {
		Map<String, Command> c = null;
		this.mud    = mud;
		try {
			c = mud.getCommands(stuffCommandset);
		} catch(NoSuchElementException e) {
			System.err.format("%s: command set not loaded, <%s>.\n", this, stuffCommandset);
		}
		commands = c;
		System.err.format("%s: waiting.\n", this);
	}

	public Mud    getMud()    { return mud; }

	public Map<String, Command> getCommands() { return commands; }

	public void setCommands(final String cmdStr) throws NoSuchElementException {
		System.err.format("%s: attempting to change the command set to %s? no thanks.\n", this, cmdStr);
	}

	public Mapper getMapper() { return mapper; }

	public Chance getChance() { return chance; }

	public void   setExit()   {
		/* fixme! on death? */
	}

	public void register(final Stuff stuff) {
		//System.err.print("Chronos::register stuff = " + stuff + "\n");
		/*if(!(stuff instanceof Mob)) {
			System.err.format("%s: that's funny, <%s>, is not a Mob; ignoring.\n", this, stuff);
			return;
		}*/
		if(active.contains(stuff)) return; /* fixme: ugh, O(n) */
		active.add(stuff);
		System.err.format("%s: registered <%s> as active.\n", this, stuff);
	}

	/* one time step */
	public void run() {
		System.err.format("<%s.run(): ", this);
		Stuff a;
		Iterator<Stuff> it = active.iterator();
		try {
			while(it.hasNext()) {
				a = it.next();
				//System.err.format("%s.hasNext = %b ", a, it.hasNext());
				System.err.format("%s ", a);
				if(a.doClockTick()) continue;
				System.err.format("(removing) ");
				it.remove();
			}
		} catch(NoSuchElementException | UnsupportedOperationException | IllegalStateException e) {
			System.err.print(this + ": confused? " + e + ".\n");
		}
		System.err.format(">\n");
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Chronos";
	}

}
