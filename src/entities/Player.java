/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.Map;
import java.util.HashMap;

import main.Connection;
import entities.Room;

/** A player; this should always have a Connection.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Player extends Character {

	static final int distanceWakeUp = 3;

	protected Connection connection;

	public Player(Connection connection, String name) {
		super();
		this.connection = connection;
		this.name  = name;
		this.title = name + " is neutral.";
	}

	@Override
	public void lookAtStuff() {
		if(in == null) return;
		for(Stuff s : in) {
			if(s == this) continue;
			connection.sendTo(s.look());
		}
	}

	/** Gives more info.
	 @return More info on the object. */
	@Override
	public String lookDetailed() {
		return name + " is connected on socket " + connection + "\nThey are wearing . . . <not implemented>";
	}

	/* Update players' bfs. */
	@Override
	protected void hasMoved() {
		if(!(in instanceof Room)) return;
		connection.getMapper().map((Room)in, distanceWakeUp, (room, dis, dir) -> {
			System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);
			//where.put(dis, room);
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
	protected Connection getConnection() {
		return connection;
	}

	/** fixme! */
	public String prompt() {
		return hpCurrent + "/" + hpTotal + " > ";
	}

}
