/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.BitVector;

/** An general object.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Object extends Stuff {

	public enum ObjectFlags {
		BREAKABLE("breakable"),
		ENTERABLE("enterable"),
		TRANSPORTABLE("transportable");
		public String symbol;
		private ObjectFlags(final String symbol) { this.symbol = symbol; }
		public String toString()                 { return symbol; }
	}
	BitVector<ObjectFlags> objectFlags = new BitVector<ObjectFlags>(ObjectFlags.class);

	/* "breakable" must have a "public isBreakable" for the BitVector magic to
	 work, etc */
	public boolean isBreakable;
	public boolean isTransportable;
	public boolean isEnterable;
	private int    mass;
	protected Room.Direction nextDir; /* for entering and controlling */

	public Object() {
		super();
		isBreakable     = false;
		isTransportable = false;
		mass            = 1;
		name  = "object";
		title = "Some sort of object is here.";
	}

	public Object(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		try {
			objectFlags.fromLine(this, in.nextLine());
		} catch(common.UnrecognisedTokenException e) {
			throw new java.text.ParseException(e.getMessage(), in.getLineNumber());
		}
	}

	public Object(final String name, final String title, final boolean isB, final boolean isT) {
		super();
		this.name = name;
		this.title = title;
		isBreakable = isB;
		isTransportable = isT;
	}

	@Override
	public boolean isTransportable() {
		return isTransportable;
	}

	@Override
	public boolean isEnterable() {
		return isEnterable;
	}

	/** Each clock tick. */
	@Override
	public boolean doClockTick() {
		//System.err.format("(Object)%s.doClockTick(): %s\n", this, nextDir);
		/* fixme: perSecond, decrement and then go */
		if(nextDir == null) return false;
		// sent already! sendToRoom(/*fixme: An(this)*/"A " + this + " is goes " + nextDir + ".");
		go(nextDir);

		// sent already! sendToRoom("A " + this + " comes in from the " + nextDir.getBack() + ".");

//		player.sendTo(sb.toString());
//		sendToContents();
//		forAllContent((stuff) -> stuff instanceof Player, (player) -> {
//			Room r = player.getRoom();
//
//		});
		nextDir = null;
		/* remove from list */
		return false;
	}

	/** Objects can have NextDir. */
	public void setNextDir(final Room.Direction where) { nextDir = where; }

}
