/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

import entities.Stuff;

/** These are hit things.
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.1, 2014-12 */
public class Hit {

	/* fixme: it would be cool to laod this at runtime */
	public enum HitType {
		trip(		"trip",		"trips",		"triped"),
		fall(		"crash",	"crashes",		"crashed"),
		crush(		"crush",	"cruses",		"crushed"),
		ballistic(	"shoot",	"shot",			"shot"),
		blunt(		"hit",		"hits",			"hit"),
		sharp(		"poke",		"pokes",		"poked"),
		slash(		"slash",	"slashes",		"slashed"),
		fire(		"burn",		"burns",		"burned"),
		ice(		"chill",	"chills",		"chilled"),
		poison(		"poison",	"poison",		"poisoned"),
		chemical(	"burn",		"burn",			"burned"),
		explosion(	"concuss",	"concuss",		"concussed"),
		electricity("electrify","electrifies",	"electrified"),
		radiation(	"irradiate","irradiates",	"irradiated"),
		magic(		"curse",	"curse",		"cursed");
		String aggressor, bystander, victim;
		HitType(final String aggressor, final String bystander, final String victim) {
			this.aggressor = aggressor;
			this.bystander = bystander;
			this.victim    = victim;
		}
	}

	HitType	hit;
	int		damage, speed;
	Stuff	victim;		/* could be null: area affect */
	Stuff	aggressor;	/* could be null: no particular thing */

	/** Constructor. */
	public Hit(final HitType hit, final int damage, final int speed, final Stuff victim, final Stuff aggressor) {
		this.hit		= hit;
		this.damage		= damage;
		this.speed		= speed;
		this.victim		= victim;
		this.aggressor	= aggressor;
	}

	/** Turns abstract thing Hit to actual damage (or not.) */
	public void hit() {
		/* fixme: calculate RIS etc */
		if(aggressor != null && victim != null) {
			aggressor.sendTo("You " + hit.aggressor + " " + victim + ".");
			aggressor.sendToRoomExcept(victim, aggressor + " " + hit.bystander + " " + victim + ".");
			victim.sendTo(aggressor + " " + hit.bystander + " you.");
			return;
		}
		if(aggressor != null) {
			aggressor.sendTo("You " + hit.aggressor + ".");
			aggressor.sendToRoom(aggressor + " " + hit.bystander + ".");
		}
		if(victim != null) {
			victim.sendTo("You are " + hit.victim + ".");
			victim.sendToRoom(victim + " is " + hit.victim + ".");
		}
	}

	/** @return A synecdochical {@link String}. */
	public String toString() {
		return "Hit " + hit + "(" + damage + ") for " + victim + " from " + aggressor;
	}

}
