/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

/** An general object.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Object extends Stuff {

	boolean isBreakable;
	boolean isTransportable;
	int     mass;

	public Object() {
		super();
		isBreakable     = false;
		isTransportable = false;
		mass            = 1;
		name  = "object";
		title = "Some sort of object is here.";
		/* name.clear() ? */
		//name.add("object");
	}

	public Object(final String name, final String title, final boolean isB, final boolean isT) {
		super();
		this.name = name;
		this.title = title;
		isBreakable = isB;
		isTransportable = isT;
	}

}
