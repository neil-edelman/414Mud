/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.Chance;
import common.BitVector;
import main.FourOneFourMud;

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

	private boolean isSleeping = true;

	public Mob() {
		super();
		title = "Someone is looking confused.";
		FourOneFourMud.getChronos().register(this);
	}

	/** Read it from a file. */
	public Mob(common.TextReader in) throws java.text.ParseException, java.io.IOException, common.UnrecognisedTokenException {
		super(in);
		mobFlags.fromLine(this, in.nextLine());
		FourOneFourMud.getChronos().register(this);
	}

	/** Do a thing. */
	public void doSomethingInteresting(Chance c) {
		
		/* go to sleep? kind of like . . .
		 connection.getMapper().map((Room)in, distanceWakeUp, (node, dis, dir) -> {
			System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);
			for(Stuff s : room) {
				if(s instanceof Player) {
					return true;
				}
			}
		});*/
		switch(c.uniform(2)) {
			case 0: go(Room.Direction.S); break;
			case 1: sendToRoomExcept(this, this + ": \"Aha!\""); break;
			default:
		}
	}

	/** Public fuction to tell if they're sleeping; used by chronos in 414Mud. */
	public boolean isSleeping() { return isSleeping; }

	/** Wake them up; eg, a Player is in the area */
	public void wakeUp() {
		isSleeping = false;
		System.err.format("%s is awake!\n", this);
	}

}
