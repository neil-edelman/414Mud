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

	private Map<Integer, Room> where = new HashMap<Integer, Room>();
	//private boolean          map[][] = new boolean[7][7];

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

	@Override
	protected void reorient() {
		/* update players' bfs */
		/*if(this instanceof Player) ((Player)this).updateWhere();*/

		where.clear();

		if(!(in instanceof Room)) return;
		connection.getMapper().map((Room)in, distanceWakeUp, (room, dis, dir) -> {
			System.err.format("%s: %s\t%d\t%s\n", this, room, dis, dir);
			where.put(dis, room);
			for(Stuff s : room) {
				/* fixme: just to be evil . . . dinosaurs can smell and hunt you! */
				if(s instanceof Mob) ((Mob)s).wakeUp();
			}
			return true;
		});
	}
	
/*	public void kill(Stuff murderer) {
		ReceiveMessage("You have been attacked and killed by " + murderer + "\n");
		
		//TODO close connection
		
	}
	
	public void UpdateLevel(String playerYouJustKilled) {
		ReceiveMessage("You have killed " + playerYouJustKilled + "\n");
		
		//TODO
		this.level++;
		this.money.AddMoney(50);
		
		ReceiveMessage("Your stats are now: " + "Level " + Integer.toString(this.level) + ", Money " + Integer.toString(this.money.GetAmount()) + "\n");
		
		
	}*/

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

	/** Does bfs to map out the rooms n hops away, where n is a constant,
	 {@link searchDepth}. Re-computed every time (it would be wasteful if n is
	 big, but the simplicity of it greatly outweights the un-optimisation.) */
/*	public void updateWhere() {
		Room node, near;
		int dist = 0, thisIncrease = 1, nextIncrease = 0;

		where.clear();
		if(in == null || !(in instanceof Room)) return;
		queue.clear();
		visited.clear();
		for(boolean isolong[] : map) { for(boolean spot : isolong) { spot = false; } }
		queue.add((Room)in);
		while(!queue.isEmpty()) {
			node = queue.remove();
			where.put(dist, node);
			visited.add(node);
			System.err.format("%d/%d where+=(%d, %s) ", thisIncrease, nextIncrease, dist, node);
			System.err.format("queue{ ");
			for(Room r : queue) System.err.format("%s ", r);
			System.err.print("}\n");
			if(dist < searchDepth) {
				for(Room.Direction dir : Room.Direction.values()) {
					if((near = node.getRoom(dir)) == null || visited.contains(near)) continue;
					queue.add(near);
					visited.add(near);
					nextIncrease++;
				}
			}
			if(--thisIncrease <= 0) {
				thisIncrease = nextIncrease;
				nextIncrease = 0;
				dist++;
			}
		}
	}*/

}
