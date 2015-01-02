/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.List;
import java.util.LinkedList;
import java.lang.reflect.Field;
import java.lang.NoSuchFieldException;

/** A room.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Room extends Stuff {

	public enum Direction {
		HERE("here", "here", 0, 0),
		N("n", "north",      0, 1),
		E("e", "east",       1, 0),
		S("s", "south",      0, -1),
		W("w", "west",      -1, 0),
		U("u", "up",         0, 0),
		D("d", "down",       0, 0);

		private String       name, var;
		private Direction    back;
		private int          x, y;
		private static final Map<String, Direction> map;

		static {

			/* reverse direction */
			HERE.back = HERE;
			N.back = S;
			S.back = N;
			E.back = W;
			W.back = E;
			U.back = D;
			D.back = U;

			/* map for turning strings into Directions
			 fixme: code duplication in BitVector */
			Map<String, Direction> mod = new HashMap<String, Direction>();
			for(Direction d : values()) {
				mod.put(d.name(), d);
				mod.put(d.var, d);
				mod.put(d.name, d);
			}
			map = Collections.unmodifiableMap(mod);

		}

		private Direction(final String var, final String name, int x, int y) {
			this.var  = var;  /* must match Field names in Room! */
			this.name = name;
			this.x    = x;
			this.y    = y;
		}
		private Field     getRoomField(Room r) throws NoSuchFieldException {
			/* fixme: I would really like to chache this */
			return r.getClass().getDeclaredField(this.var);
		}
		private Room      getRoom(Room r)              {
			try {
				return (Room)getRoomField(r).get(r);
			} catch(NoSuchFieldException | IllegalAccessException e) {
				System.err.format("%s.%s: %s.\n", r, this, e);
				return null;
			}
		}
		public Direction getBack()                     { return back; }
		public String    toString()                    { return name; }
		public int getX()                              { return x; }
		public int getY()                              { return y; }
		public static Direction find(final String str) { return map.get(str); }
	};

	/* the members of Room; the directions are essential to the above */
	protected Room here, n, e, s, w, u, d;
	protected String description;

	@Override
	public Room getRoom(Direction dir) { return dir.getRoom(this); }

	@Override
	public Room getRoom() { return this; };

	public Room() {
		super();
		here        = this;
		name        = "room";
		title       = "A room.";
		description = "This is an entrely bland room.";
	}

	/** Read it from a file. */
	public Room(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		here        = this;
		description = in.nextParagraph();
	}

	public Room(final String name, final String title, final String desc) {
		super();
		here             = this;
		this.name        = name;
		this.title       = title;
		this.description = desc;
	}

	public void setDirection(Direction dir, Room target) {
		try {
			dir.getRoomField(this).set(this, target);
		} catch(NoSuchFieldException | IllegalAccessException e) {
			System.err.format("%s: trying to setDirection %s to %s; %s.\n", this, dir, target, e);
		}
	}

	public void connectDirection(Direction dir, Room target) {
		this.setDirection(dir, target);
		target.setDirection(dir.getBack(), this);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String look() {
		StringBuilder sb = new StringBuilder();
		sb.append(title);
		sb.append(" (");
		sb.append(name);
		sb.append(")\n");
		exits(sb);
		for(Stuff i : this) {
			sb.append("\n");
			sb.append(i.look());
		}
		return sb.toString();
	}

	@Override
	public String lookDetailed(final Stuff exempt) {
		StringBuilder sb = new StringBuilder();
		sb.append("\33[4m");
		sb.append(title);
		sb.append("\33[0m (");
		sb.append(name);
		sb.append(")\n");
		sb.append(description);
		sb.append("\n");
		exits(sb);
		for(Stuff i : this) {
			if(i == exempt) continue;
			sb.append("\n");
			sb.append(i.look());
		}
		return sb.toString();
	}

	private void exits(StringBuilder sb) {
		sb.append("exits \33[7m[ ");
		if(n != null) sb.append("n ");
		if(e != null) sb.append("e ");
		if(s != null) sb.append("s ");
		if(w != null) sb.append("w ");
		if(u != null) sb.append("u ");
		if(d != null) sb.append("d ");
		sb.append("]\33[0m");
	}

}
