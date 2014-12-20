/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.List;
import java.util.LinkedList;

/** A container.
 
 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Container extends Object {

	public int massLimit;
	public List<Stuff> contents = new LinkedList<Stuff>();

	public Container() {
		super();
		massLimit = 10;
		name = "container";
		title= "Some sort of container is here.";
		/* name.clear() */
		/*name.add("container");*/
	}

}
