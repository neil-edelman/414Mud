/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import common.BitVector;

/** An general object.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Object extends Stuff {

	public enum ObjectFlags {
		BREAKABLE("breakable"),
		TRANSPORTABLE("transportable");
		public String symbol;
		private ObjectFlags(final String symbol) { this.symbol = symbol; }
		public String toString()                 { return symbol; }
	}
	BitVector<ObjectFlags> objectFlags = new BitVector<ObjectFlags>(ObjectFlags.class);

	public boolean isBreakable;
	public boolean isTransportable;
	private int    mass;

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

	public Object(common.TextReader in) throws java.text.ParseException, java.io.IOException, common.UnrecognisedTokenException {
		super(in);
		objectFlags.fromLine(this, in.nextLine());
	}

	public Object(final String name, final String title, final boolean isB, final boolean isT) {
		super();
		this.name = name;
		this.title = title;
		isBreakable = isB;
		isTransportable = isT;
	}

}
