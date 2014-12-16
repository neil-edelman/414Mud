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

/* Serializ is for binary file mostly, I think */
public class Stuff implements Iterable<Stuff> /*, Serializable*/ {

	//private static int vnumCounter = 0;

	//public int vnum;
	/*public List<String> name = new LinkedList<String>(); <- only one name is fine */
	protected String name;  /* lower case */
	protected String title; /* sentence case */

	protected List<Stuff> contents = new LinkedList<Stuff>();
	protected Stuff in;

	Stuff() {
		//vnum = ++vnumCounter;
		//name.add("stuff");
		name  = "stuff";
		title = "Some stuff is here.";
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

	public void transportTo(final Stuff container) {
		if(in != null) sendToRoom(this + " disapparates.");
		placeIn(container);
		sendTo("You disapparate and instantly travel to '" + container.title + "'");
		sendToRoom(this + " suddenly re-apparates.");
	}

	protected void placeIn(Stuff container) {
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
	protected void sendTo(final String message) {
	}

	public void sendToRoom(final String message) {
		if(this.in == null) return;
		this.in.sendToContentsExcept(this, message);
	}

	private void sendToContentsExcept(final Stuff except, final String message) {
		for(Stuff s : this) {
			if(s == except) continue;
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
	protected Room getRoom(Direction dir) { return null; }

	/** Prints all the data so it will be serialisable (but in text, not binary.)
	 @return Blank string; fixme: the serialised version. */
	public String saveString() {
		return ""; //<------
	}

}
