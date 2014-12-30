/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.Map;
import java.util.HashMap;

//import java.util.Iterator;

import main.Connection;
import entities.Room;
import main.Mapper;
import main.Mud;

/** A player; this should always have a Connection.

 @author	Sid, Neil
 @version	1.1, 2014-12
 @since		1.0, 2014-11 */
public class Player extends Character /*implements PlayerLike*/ {

	static final int distanceWakeUp = 3;

	protected Connection connection;

	/** The defaut name. */
	public Player(Connection connection) {
		super();
		this.connection = connection;
		name = "Nemo"; /* connection.getName? */
		title= "Nemo hasn't chosen a name yet.";
	}

	/*public Player(Connection connection, String name) {
		super();
		this.connection = connection;
		this.name  = name;
		this.title = name + " is here.";
	}*/

	/** Part of the contract with GetHandler: this is in it's own thread, so it
	 must use it's own Handler, viz, Connection implemts Mud.Handler */
	public Mud.Handler getHandler() {
		return connection;
	}
	/*public String getHandlerName() {
		return name + "(" + connection + ")";
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
		getHandler().getMapper().map((Room)in, distanceWakeUp, (room, dis, dir) -> {
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

	/** fixme! */
	public String getPrompt() {
		return hpCurrent + "/" + hpTotal + " > ";
	}

}
