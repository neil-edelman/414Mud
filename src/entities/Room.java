/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 A room.
 
 @author Sid, Neil
 @version 1
 @since 2014 */

package entities;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.List;
import java.util.LinkedList;
import java.lang.reflect.Field;
import java.lang.NoSuchFieldException;

public class Room extends Stuff {

	protected String description;
	protected Room n, e, s, w, u, d;

	/* I love and hate Java. -Neil */
	public enum Direction {
		N("north"), E("east"), S("south"), W("west"), U("up"), D("down");
		private String       name;
		private Direction    back;
		private static Field room;
		private static final Map<String, Direction> map;
		static {
			/* reverse direction */
			N.back = S;
			S.back = N;
			E.back = W;
			W.back = E;
			U.back = D;
			D.back = U;
			/* the actual rooms */
			try {
				N.room = Room.class.getDeclaredField("n");
				E.room = Room.class.getDeclaredField("e");
				S.room = Room.class.getDeclaredField("s");
				W.room = Room.class.getDeclaredField("w");
				U.room = Room.class.getDeclaredField("u");
				D.room = Room.class.getDeclaredField("d");
			} catch(NoSuchFieldException e) {
				System.err.format("Direction: inconceivable! %s.\n", e);
			}
			/* map for turning strings into Directions */
			Map<String, Direction> mod = new HashMap<String, Direction>();
			for(Direction d : values()) mod.put(d.name(), d);
			map = Collections.unmodifiableMap(mod);
		}
		private Direction(final String name)           { this.name = name; }
		public  Direction getBack()                    { return back; }
		private Room      getRoom(Room r)              {
			/* so so so sad! why won't it work? :[ */
			/*try {
				Room d = (Room)room.get(r);
				System.err.print("getRoom: '" + r + "' accessing " + room + " = " + d + " (always null :[.)\n");
			} catch(IllegalAccessException e) {
				System.err.print("Direction: illegal; " + e + ".\n");
				return null;
			}*/
			/* this is a hack :[ */
			switch(this) {
				case N: return r.n;
				case E: return r.e;
				case S: return r.s;
				case W: return r.w;
				case U: return r.u;
				case D: return r.d;
			}
			return null;
		}
		public String toString()                       { return name; }
		public static Direction find(final String str) { return map.get(str); }
	};

	@Override
	protected Room getRoom(Direction dir) { return dir.getRoom(this); }

	public Room() {
		super();
		name        = "room";
		title       = "A room.";
		description = "This is an entrely bland room.";
	}

	public Room(final String name, final String title, final String desc) {
		super();
		this.name        = name;
		this.title       = title;
		this.description = desc;
	}

	public void setDirection(Direction dir, Room target) {
		/* also a hack :[ */
		switch(dir) {
			case N: n = target; break;
			case E: e = target; break;
			case S: s = target; break;
			case W: w = target; break;
			case U: u = target; break;
			case D: d = target; break;
		}
	}
	public void connectDirection(Direction dir, Room target) {
		this.setDirection(dir, target);
		target.setDirection(dir.getBack(), this);
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String lookDetailed() {
		return "(" + name + ") " + title + "\n" + description + "\nexits [ "
		+ (n != null ? "n " : "")
		+ (e != null ? "e " : "")
		+ (s != null ? "s " : "")
		+ (w != null ? "w " : "")
		+ (u != null ? "u " : "")
		+ (d != null ? "d " : "")
		+ "]";
	}

}
