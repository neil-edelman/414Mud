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
		N("n", "north"),
		E("e", "east"),
		S("s", "south"),
		W("w", "west"),
		U("u", "up"),
		D("d", "down");

		private String       name, var;
		private Direction    back;
		private static final Map<String, Direction> map;

		static {

			/* reverse direction */
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

		private Direction(final String var, final String name) {
			this.var  = var;  /* must match Field names in Room! */
			this.name = name;
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
		public static Direction find(final String str) { return map.get(str); }
	};

	/* the members of Room; the directions are essential to the above */
	protected Room n, e, s, w, u, d;
	protected String description;

	@Override
	public Room getRoom(Direction dir) { return dir.getRoom(this); }

	public Room() {
		super();
		name        = "room";
		title       = "A room.";
		description = "This is an entrely bland room.";
	}

	/** Read it from a file. */
	public Room(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		super(in);
		description = in.nextParagraph();
	}

	public Room(final String name, final String title, final String desc) {
		super();
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
