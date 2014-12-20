/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

/** NPC.

@author	Sid, Neil
@version	1.0, 11-2014
@since		1.0, 11-2014 */
public class Mob extends Character {

	public boolean isFriendly;
	public boolean isXeno;

	public Mob() {
		super();
		title = "Someone is looking confused.";
	}

	public Mob(final String name, final String title, final boolean isF, final boolean isX) {
		super();
		this.name = name;
		this.title = title;
		isFriendly = isF;
		isXeno = isX;
	}

}
