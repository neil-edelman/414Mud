/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package entities;

import java.util.List;
import java.util.LinkedList;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.Iterator;

import main.Connection;
import main.Mapper;
import entities.Room.Direction;
import main.Mud;

/** The most general sort of stuff.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Stuff implements Iterable<Stuff> {

	private static final int searchDepth = 3;

	protected String name;  /* lower case */
	/* fixme: have a hash for when there's the collisions */
	protected String title; /* sentence case */

	/* fixme: have TreeMap or something */
	protected List<Stuff> contents = new LinkedList<Stuff>();
	protected Stuff in;

	public Stuff() {
		name     = "stuff";
		title    = "Some stuff is here.";
	}

	/** Read it from a file. */
	public Stuff(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		name  = in.nextLine();
		title = in.nextLine();
	}

	public void setName(final String name) {
		String old = this.name;
		this.name = name;
		sendToRoom("'" + old + "' is now known as '" + this + ".'");
		sendTo("The universe dubs thee, '" + this + ".'");
	}

	public void setTitle(final String line) {
		title = line;
		sendToRoom(this + " is now '" + title + "'");
		sendTo("You are now '" + title + "'");
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public Mapper getMapper() {
		return Mud.getMudInstance().getChronos().getMapper();
	}

	/** Noisy (mostly use this.) */
	public void transportTo(final Stuff container) {
		sendToRoom(this + " suddenly disapparates.");
		placeIn(container);
		sendTo("You disapparate and instantly travel to '" + container + "'\n"); /* newline! we are going somewhere else */
		sendToRoom(this + " suddenly apparates.");
		/* players will want to look around immediatly */
		sendTo((container != null) ? container.lookDetailed(this) : "Endless blackness surrounds you; you suddenly feel weightless.");
	}

	/** Silent (only if you know what you're doing.) */
	public void placeIn(Stuff container) {
		Connection connection;

		/* we're already in something */
		if(in != null) {
			in.contents.remove(this);
			in = null;
		}

		/* appear somewhere else */
		in = container;
		if(container != null) container.contents.add(this);

		hasMoved();

		//System.err.print(this + " in " + container + ".\n");
	}

	/** Called by {@link placeIn} to do stuff. */
	protected void hasMoved() {
		/* do nothing */
	}

	public Stuff getIn() {
		return in;
	}

	/** Overwrote on things that have a connection.
	 @param message
		The string to send; no newline. */
	public void sendTo(final String message) {
	}

	public void sendToRoom(final String message) {
		if(this.in == null) return;
		this.in.sendToContentsExcept(this, message);
	}

	public void sendToRoomExcept(final Stuff except, final String message) {
		if(this.in == null) return;
		this.in.sendToContentsExcept(this, except, message);
	}

	public void sendToContentsExcept(final Stuff except, final String message) {
		for(Stuff s : this) {
			if(s == except) continue;
			s.sendTo(message);
		}
	}

	/** fixme: could be more optimised */
	private void sendToContentsExcept(final Stuff except1, final Stuff except2, final String message) {
		for(Stuff s : this) {
			if(s == except1 || s == except2) continue;
			s.sendTo(message);
		}
	}

	public void sendToContents(final String message) {
		for(Stuff s : this) s.sendTo(message);
	}

	/** @return The connection, if there is one, otherwise null. */
	public Mud.Handler getHandler() {
		return Mud.getMudInstance().getChronos();
	}

	/** @return How the object looks very simply. */
	public String toString() {
		return name;
	}

	/** Gives more info.
	 @return More info on the object. */
	public String look() {
		return "(" + name + ") " + title;
	}

	/** Gives detailed info.
	 @param exempt	It would be akward if you looked and saw yourself.
	 @return		String info. */
	public String lookDetailed(final Stuff exempt) {
		StringBuilder sb = new StringBuilder();
		sb.append(look());
		for(Stuff i : this) {
			if(i == exempt) continue;
			sb.append("\n");
			sb.append(i.look());
		}
		return sb.toString();
	}

	/** @return Iterate over the contents. */
	public Iterator<Stuff> iterator() {
		return contents.iterator();
	}

	/* @return Null since there is no directions (overwriten in Room.) */
	public Room getRoom(Direction dir) { return null; }

	/** Sets the flags from a line
	 @param line	The line. */
	public void setFlags(final String line) {
	}

	public boolean isTransportable() {
		return false;
	}

	public void go(Room.Direction where) {
		if(in == null) {
			sendTo("Can't do that; you are floating in space.");
			return;
		}
		Room target = in.getRoom(where);
		if(target == null) {
			sendTo("You can't go that way.");
			sendToRoom(this + " searches for a way " + where + ".");
			return;
		}
		sendTo("You walk " + where + ".\n"); /* newline! helps keep room seperate */
		sendToRoom(this + " walks " + where + ".");
		placeIn(target);
		sendToRoom(this + " walks in from the " + where.getBack() + ".");
		sendTo(target.lookDetailed(this));
	}

	/** fixme!!! it's annoying; more advanced please! */
	public Stuff matchContents(String obj) {
		/* lol O(n) badness */
		for(Stuff stuff : this) {
			if(obj.equals(stuff.getName())) return stuff;
		}
		return null;
	}

	/** tests if the target is enterable */
	public boolean isEnterable() {
		return false;
	}

	/** Heirarcy! */
	public void enter(Stuff target, boolean isIn) {
		sendTo("You get " + (isIn ? "in" : "on") + " the " + target + ".");
		sendToRoom(this + " gets up and rides the " + target + ".");
		placeIn(target);
		sendToRoom(this + " gets up here.");
	}

	/** Prints all the data so it will be serialisable (but in text, not binary.)
	 @return Blank string; fixme: the serialised version. */
	public String saveString() {
		return ""; //<------
	}

}
