/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.Map;
import java.util.HashMap;

import main.Connection;
import entities.Room;
import main.Mapper;
import main.Mud;

/** A player; this should always have a Connection.

 @author	Sid, Neil
 @version	1.1, 2014-12
 @since		1.0, 2014-11 */
public class Player extends Character {

	static final int distanceWakeUp = 3;

	protected Connection connection;

	/** The defaut name. */
	public Player(Connection connection) {
		super();
		this.connection = connection;
		name = "Nemo"; /* connection.getName()? */
		title= "Nemo hasn't chosen a name yet.";
		hpTotal = hpCurrent = 1;
		level = 0;
		money = 0;
	}

	/** @return	This is in it's own thread, so it must use it's own Handler. */
	@Override
	public Mud.Handler getHandler() {
		return connection;
	}

	/** @return More info on the object. */
	@Override
	public String lookDetailed(final Stuff exempt) {
		StringBuilder sb = new StringBuilder();
		/* you can look at yourself */
		if(this == exempt) sb.append("You look at yourself; ");
		sb.append(name);
		sb.append(" is connected on socket ");
		sb.append(connection);
		sb.append("\nThey are wearing . . . <not implemented>");
		return sb.toString();
	}

	/* Update players' bfs. */
	@Override
	protected void hasMoved() {
		StringBuilder sb = new StringBuilder("\n");
		Room r;

		/* create appropriate message */
		if((r = getRoom()) == null) {
			sb.append("Endless blackness surrounds you; you suddenly feel weightless.");
		} else {
			sb.append(r.lookDetailed(this));
		}
		sendTo(sb.toString());

		/* wake up mobs */
		getHandler().getMapper().map(r, distanceWakeUp, (room, dis, dir) -> {
			System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);
			//where.put(dis, room);
			/* fixme: have separite lists for mobs, players, and stuff; be careful */
			for(Stuff s : room) {
				/* fixme: just to be evil . . . dinosaurs can smell and hunt you! */
				if(s instanceof Mob) ((Mob)s).wakeUp();
			}
			return true;
		});
	}

	@Override
	public void sendTo(final String message) {
		connection.sendTo(message);
	}

	@Override
	public void levelUp() {
		level++;
		int hpOld = hpTotal;
		int hpNew = 50 * level + 100;
		if(hpNew > hpTotal) hpTotal = hpNew;
		int hpGain = hpTotal - hpOld;
		hpCurrent += hpGain;
		sendTo("You level up to " + level + "; health: " + hpOld + "->" + hpTotal + " (imporoved " + hpGain + ")");
	}

	/** fixme! */
	public String getPrompt() {
		return hpCurrent + "/" + hpTotal + " > ";
	}

}
