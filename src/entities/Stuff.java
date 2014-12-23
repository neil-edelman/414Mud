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
import entities.Room.Direction;

/** The most general sort of stuff.

 @author	Sid, Neil
 @version	1.0, 11-2014
 @since		1.0, 11-2014 */
public class Stuff implements Iterable<Stuff> /*, Serializable*/ {

	// fixme: the [ exits ] should always be visible

	/*public List<String> name = new LinkedList<String>(); <- only one name is fine */
	protected String name;  /* lower case */
	protected String title; /* sentence case */

	/* fixme: have TreeMap or something */
	protected List<Stuff> contents = new LinkedList<Stuff>();
	protected Stuff in;

	public Stuff() {
		name  = "stuff";
		title = "Some stuff is here.";
	}

	/** Read it from a file. */
	public Stuff(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		name  = in.nextLine();
		title = in.nextLine();
	}

	public void setName(final String name) {
		String old = this.name;
		this.name = name;
		sendToRoom("The '" + old + "' is now known as '" + this + ".'");
		sendTo("You will not be called '" + old + ";' I dub thee '" + this + ".'");
	}

	public void setTitle(final String line) {
		this.title = title;
		sendToRoom(this + " is now '" + title + ".'");
		sendTo("You are '" + title + ".'");
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	/** Noisy (mostly use this.) */
	public void transportTo(final Stuff container) {
		if(in != null) sendToRoom(this + " disapparates.");
		placeIn(container);
		sendTo("You disapparate and instantly travel to '" + container.title + "'");
		sendToRoom(this + " suddenly apparates.");
		/* players will want to look around immediatly */
		Connection c = this.getConnection();
		if(c == null) return;
		c.sendTo(container.lookDetailed());
		lookAtStuff();
	}

	/** Silent (only if you know what you're doing.) */
	public void placeIn(Stuff container) {
		/* it's already in something */
		if(in != null) {
			in.contents.remove(this);
			in = null;
		}

		/* appear somewhere else */
		in = container;
		container.contents.add(this);

		//System.err.print(this + " in " + container + ".\n");
	}

	public void lookAtStuff() { }

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

	private void sendToContentsExcept(final Stuff except, final String message) {
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
	protected Connection getConnection() {
		return null;
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
	 @return String info. */
	public String lookDetailed() {
		return look();
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

	/** Prints all the data so it will be serialisable (but in text, not binary.)
	 @return Blank string; fixme: the serialised version. */
	public String saveString() {
		return ""; //<------
	}

}
