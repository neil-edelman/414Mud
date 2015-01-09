/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.Chance;
import common.BitVector;
import main.Mapper;
import main.Mud;

/** NPC.

@author	Sid, Neil
@version	1.1, 12-2014
@since		1.0, 11-2014 */
public class Mob extends Character {

	public enum MobFlags implements BitVector.Flags {
		FRIENDLY("friendly"),
		XENO("xeno");
		public String symbol;
		private MobFlags(final String symbol) { this.symbol = symbol; }
		public String toString()              { return symbol; }
	}
	private BitVector<MobFlags> mobFlags = new BitVector<MobFlags>(MobFlags.class);

	public boolean isFriendly;
	public boolean isXeno;

	private int countDown = 0;

	public Mob() {
		super();
		title = "Someone is looking confused.";
		// fixme: is there anyone around? register. same f'n as below
	}

	/** Read it from a file. */
	public Mob(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		try {
			mobFlags.fromLine(this, in.nextLine());
		} catch(common.UnrecognisedTokenException e) {
			throw new java.text.ParseException(e.getMessage(), in.getLineNumber());
		}
	}

	/** Do a thing.
	 @return	Whether it continues to be on the stuff list. */
	@Override
	public boolean doClockTick() {
		Room r;

		if(countDown > 0) {
			System.err.format("%s: countdown %d.\n", this, countDown);
			countDown--;
			return true;
		}

		/* the mob is in space, deactivate it */
		if((r = getRoom()) == null) return false;

		/* use two times */
		Mud.Handler handler = getHandler();

		/* go to sleep?
		 fixme: this should happen much less frequently
		 fixme: have Player, Mob, Other lists, then this becomes
		 room.playerList.isEmpty(), O(1) instead of O(n) for @ room; but
		 tricky beacuse of, eg, Players in Mobs */
		/* fixme: const static ^ */
		if(handler.getMapper().map(r, Player.distanceWakeUp, (room, dis, dir) -> {
			return room.isAllContent((s) -> !(s instanceof Player));
		})) {
			System.err.format("%s: no one nearby, going to sleep.\n", this);
			return false;
		}

		/* random walking into walls */
		switch(handler.getChance().uniform(1)) {
			case 0: go(Room.Direction.S); break;
			case 1: sendToRoomExcept(this, this + ": \"Aha!\""); break;
			default:
		}
		/* the time between events in a poisson process is an exponential random
		 variable; \beta = avg time b/t events */
		/* fixme: not a geiger counter; uniform with markov? */
		countDown = handler.getChance().exponential(10);
		return true;
	}

	/** fixme: you should so be able to, eg, "ride dragon" */
	@Override
	public boolean isEnterable() {
		return false;
	}

}
