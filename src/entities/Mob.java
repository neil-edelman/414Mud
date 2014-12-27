/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.Chance;
import common.BitVector;
import main.Mud;

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
		Mud.getChronos().register(this);
	}

	/** Read it from a file. */
	public Mob(common.TextReader in) throws java.text.ParseException, java.io.IOException, common.UnrecognisedTokenException {
		super(in);
		mobFlags.fromLine(this, in.nextLine());
		Mud.getChronos().register(this);
	}

	/** Do a thing. */
	public void doSomethingInteresting(Chance c) {

		/* go to sleep? fixme: this should happen much less frequently */
		if(Mud.getChronos().getMapper().map((Room)in, Player.distanceWakeUp, (room, dis, dir) -> {
			/*System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);*/
			for(Stuff s : room) {
				if(s instanceof Player) {
					System.err.format("%s: staying awake because of %s.\n", this, s);
					return false;
				}
			}
			return true;
		})) {
			isSleeping = true;
			System.err.format("%s: no one nearby, going to sleep.\n", this);
			return;
		}

		/* random walking into walls */
		switch(c.uniform(1)) {
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

	/** fixme: you should so be able to, eg, "ride dragon" */
	@Override
	public boolean isEnterable() {
		return false;
	}

}
