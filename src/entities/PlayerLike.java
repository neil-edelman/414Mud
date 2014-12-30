/** Copyright 2014 Neil Edelman, distributed under the terms of the GNU General
 Public License, see copying.txt */

package entities;

import main.Mud;

/** Implemented by Player and Connection;
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.1, 2014-12 */
public interface PlayerLike extends Character /*implements Iterable<Stuff>, Mud.GetHandler*/ {
	public Mud.Handler getHandler();
	public String getPrompt();
}
