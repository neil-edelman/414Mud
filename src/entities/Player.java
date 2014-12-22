/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import main.Connection;
import entities.Room;

/** A player; this should always have a Connection.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Player extends Character {

	protected Connection connection;

	public Player(Connection connection, String name) {
		super();
		this.connection = connection;
		this.name  = name;
		this.title = name + " is neutral.";
	}

	public void go(Room.Direction where) {
		if(in == null) {
			sendTo("Can't do that; you are floating in space.");
			return;
		}
		Room target = in.getRoom(where);
		if(target == null) {
			sendTo("You can't go that way.");
			return;
		}
		sendToRoom(this + " walks " + where + ".");
		sendTo("You walk " + where + ".");
		placeIn(target);
		sendToRoom(this + " walks in from " + where.getBack() + ".");
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

}
