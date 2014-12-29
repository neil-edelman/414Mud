/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.util.List;
import java.util.LinkedList;

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

	private Chance  chance = new Chance();
	private Mapper  mapper = new Mapper();
	private List<Mob> mobs = new LinkedList<Mob>(); /* <- hashmap!!? */

	/** Constructor. */
	public Chronos() {
	}

	public void register(Stuff stuff) {
		if(!(stuff instanceof Mob)) {
			System.err.format("%s: that's funny, <%s>, is not a Mob; ignoring.\n", this, stuff);
			return;
		}
		mobs.add((Mob)stuff);
		System.err.format("%s: registered <%s>.\n", this, stuff);
	}

	public Mapper getMapper() {
		return mapper;
	}

	/* one time step */
	public void run() {
		System.out.format("<Bump>\n");
		for(Mob m : mobs) {
			if(m.isSleeping()) continue;
			m.doSomethingInteresting(chance);
		}
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
		return "Chronos";
	}

}
