/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

/** An abstract character.
 
 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Character extends Stuff {

	/* fixme: have alignment, aggresive, plug into formula */
	public int hpTotal;
	public int hpCurrent;
	public int level; /* fixme: not used */
	public int money;

	public Character() {
		super();
		hpTotal = hpCurrent = 50;
		level = 1;
		money = 50;
		name = "someone";
		title= "Someone is chilling.";
	}

	public Character(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		/* fixme: stuff here */
		hpTotal = hpCurrent = 50;
		level = 1;
		money = 50;
	}

	/* fixme; put in Stuff.class */
	public void kill(Stuff murderer) {
		murderer.sendTo("You have slain " + this + "!");
		murderer.sendToRoom(this + " has been attacked and killed by " + murderer + ".");
	}

}
