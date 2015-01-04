/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.BitVector;

/** A wearable object.

 @author	Neil
 @version	1.1, 01-2015
 @since		1.1, 01-2015 */
public class Equipment extends Object {

	public enum EquipmentFlags implements BitVector.Flags {
		BREAKABLE("breakable");
		public String symbol;
		private EquipmentFlags(final String symbol) { this.symbol = symbol; }
		public String toString()                 { return symbol; }
	}
	BitVector<ObjectFlags> objectFlags = new BitVector<ObjectFlags>(ObjectFlags.class);

	/* fixme: it would be cool to load this at runtime, probably a hack
	 involving reflection */
	private enum HitType {
		/*pugilism(	"puch",		"punches",		"punched"), Your punch hits -> blunt */
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

	/* "breakable" must have a "public isBreakable" for the BitVector magic to
	 work, etc */
	public boolean isBreakable;
	public boolean isTransportable;
	public boolean isEnterable;
	private int    mass;
	protected Room.Direction nextDir; /* for entering and controlling */

	public Equipment() {
		super();
		isBreakable     = true;
		isTransportable = true;
		mass            = 1;
		name  = "object";
		title = "Some sort of object is here.";
	}

	public Equipment(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		try {
			objectFlags.fromLine(this, in.nextLine());
		} catch(common.UnrecognisedTokenException e) {
			throw new java.text.ParseException(e.getMessage(), in.getLineNumber());
		}
	}

	/** Each clock tick. */
	@Override
	public boolean doClockTick() {
		/* remove */
		return false;
	}

}
