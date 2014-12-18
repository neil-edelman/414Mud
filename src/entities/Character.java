/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 An abstract character.
 
 @author Sid, Neil
 @version 1
 @since 2014 */

package entities;

public class Character extends Stuff {

	/* fixme: have alignment, aggresive, plug into formula */
	public int totalhp;
	public int hp;
	public int level; /* fixme: not used */
	public int money;

	public Character() {
		super();
		totalhp = hp = 50;
		level = 1;
		money = 50;
		name = "someone";
		title= "Someone is chilling.";
	}

	/* fixme; put in Stuff.class */
	public void kill(Stuff murderer) {
		murderer.sendTo("You have slain " + this + "!");
		murderer.sendToRoom(this + " has been attacked and killed by " + murderer + ".");
	}

}
