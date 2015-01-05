/* Copyright 2015 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package main;

import java.io.IOException;
import java.text.ParseException;

import common.TextReader;
import entities.Stuff;
import main.Mud;

/** Damage types.
 
 @author	Neil
 @version	1.1, 2015-01
 @since		1.1, 2014-12 */
public class Damage {

	String aggressor, bystander, victim;

	public Damage(final TextReader in) throws ParseException, IOException {
		aggressor = in.readLine();
		bystander = in.readLine();
		victim    = in.readLine();
		if(in.readLine() != null) throw new ParseException("expecting EOF", in.getLineNumber());
		if(Mud.isVerbose) System.err.printf("Damage: %s, %s, %s.\n", aggressor, bystander, victim);
	}

	/** Hit Stuff.
	 @param aggessor	The one with the big stick.
	 @param victim		The one getting hit. Can be null for area attacks? */
	public static void hit(final Stuff aggressor, final Stuff victim, final Damage damage) {
		/* figure out which weapons to use */
		/* fixme: calculate RIS etc */
		if(aggressor != null && victim != null) {
			aggressor.sendTo("You " + damage.aggressor + " " + victim + ".");
			aggressor.sendToRoomExcept(victim, aggressor + " " + damage.bystander + " " + victim + ".");
			victim.sendTo(aggressor + " " + damage.bystander + " you.");
			return;
		}
		if(aggressor != null) {
			aggressor.sendTo("You " + damage.aggressor + ".");
			aggressor.sendToRoom(aggressor + " " + damage.bystander + ".");
		}
		if(victim != null) {
			victim.sendTo("You are " + damage.victim + ".");
			victim.sendToRoom(victim + " is " + damage.victim + ".");
		}
	}

}
