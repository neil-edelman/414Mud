/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.BitVector;

/** NPC.

@author	Sid, Neil
@version	1.1, 12-2014
@since		1.0, 11-2014 */
public class Mob extends Character {

	public enum MobFlags {
		FRIENDLY("friendly"),
		XENO("xeno");
		public String symbol;
		private MobFlags(final String symbol) { this.symbol = symbol; }
		public String toString()              { return symbol; }
	}
	private BitVector<MobFlags> mobFlags = new BitVector<MobFlags>(MobFlags.class);

	public boolean isFriendly;
	public boolean isXeno;

	public Mob() {
		super();
		title = "Someone is looking confused.";
	}

	/** Read it from a file. */
	public Mob(common.TextReader in) throws java.text.ParseException, java.io.IOException, common.UnrecognisedTokenException {
		super(in);
		mobFlags.fromLine(this, in.nextLine());
	}

}
