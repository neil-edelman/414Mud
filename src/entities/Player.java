/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.Map;
import java.util.HashMap;

import main.Connection;
import entities.Room;
import main.Mapper;

/** A player; this should always have a Connection.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Player extends Character {

	static final int distanceWakeUp = 3;

	protected Connection connection;

	/** The defaut name. */
	public Player(Connection connection) {
		super();
		this.connection = connection;
		name = "nemo"; /* fixme: Orcish */
		title= "Nemo hasn't chosen a name yet.";
	}

	/*public Player(Connection connection, String name) {
		super();
		this.connection = connection;
		this.name  = name;
		this.title = name + " is neutral.";
	}*/

	/** Gives more info.
	 @return More info on the object. */
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
		if(!(in instanceof Room)) return;
		getMapper().map((Room)in, distanceWakeUp, (room, dis, dir) -> {
			System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);
			//where.put(dis, room);
			for(Stuff s : room) {
				/* fixme: just to be evil . . . dinosaurs can smell and hunt you! */
				if(s instanceof Mob) ((Mob)s).wakeUp();
			}
			return true;
		});
	}

	/** Overrides Chronos: this is in the other thread, so it must use it's own */
	@Override
	public Mapper getMapper() {
		System.err.print(" ****** Returning " + connection + ".getMapper()\n");
		return connection.getMapper();
	}

	@Override
	public void sendTo(final String message) {
		connection.sendTo(message);
	}

	@Override
	public Connection getConnection() {
		return connection;
	}

	/** fixme! */
	public String prompt() {
		return hpCurrent + "/" + hpTotal + " > ";
	}

}
