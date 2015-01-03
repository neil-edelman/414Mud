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
import java.util.function.Predicate;

import main.Connection;
import main.Mapper;
import main.Command;
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

	/** Create it with default values. */
	public Stuff() {
		name     = "stuff";
		title    = "Some stuff is here.";
	}

	/** @param in	Read it from a file. */
	public Stuff(common.TextReader in) throws java.text.ParseException, java.io.IOException {
		name  = in.nextLine();
		title = in.nextLine();
	}

	/** Each clock tick. */
	public boolean doClockTick() {
		System.err.print("%s: on active list? removing.");
		return false;
	}

	/** @return		The defaut timer-Handler-thread thing. */
	public Mud.Handler getHandler() {
		return Mud.getMudInstance().getChronos();
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

	/** Noisy wrap of {@link #placeIn} (ie, writes what it's doing to the
	 screen.) */
	public void transportTo(final Stuff container) {
		sendToRoom(this + " suddenly disapparates.");
		sendTo("You disapparate and instantly travel to '" + container + "'");
		placeIn(container);
		sendToRoom(this + " suddenly apparates.");
		/* players will want to look around immediatly; don't forget the newline */
		sendTo((container != null) ? "\n" + container.lookDetailed(this)
			   : "\nEndless blackness surrounds you; you suddenly feel weightless.");
	}

	/** @param container	Silently places this in container with no checks.
	 @bug					Check for DAG; if not, {@link getRoom()} could be
							infinitly recusing. */
	public void placeIn(Stuff container) {
		Connection connection;

		//System.err.format("%s.placeIn(%s);\n", this, container);

		/* we're already in something */
		if(in != null) {
			in.contents.remove(this);
			in = null;
		}

		/* appear somewhere else */
		in = container;
		if(container != null) container.contents.add(this);

		hasMoved();

	}

	/** Called by {@link placeIn} to do stuff specific all the different
	 entities. */
	protected void hasMoved() {
		if(!isEnterable()) return;
		/* recurse */
		System.err.format("(%s.hasMoved() { ", this);
		for(Stuff i : this) {
			System.err.format("%s ", i);
			i.hasMoved();
		}
		System.err.format(">}) ");
	}

	/** @return		The one above it in the Stuff tree. Can be null. */
	public final Stuff getIn() {
		return in;
	}

	/** Overwrote on things that have a connection; viz, Player.
	 @param message
		The string to send; no newline. */
	public void sendTo(final String message) {
	}

	public final void sendToRoom(final String message) {
		if(this.in == null) return;
		this.in.sendToContentsExcept(this, message);
	}

	public final void sendToRoomExcept(final Stuff except, final String message) {
		if(this.in == null) return;
		this.in.sendToContentsExcept(this, except, message);
	}

	public final void sendToContentsExcept(final Stuff except, final String message) {
		for(Stuff s : this) {
			if(s == except) continue;
			s.sendTo(message);
		}
	}

	/** fixme: could be more optimised */
	private final void sendToContentsExcept(final Stuff except1, final Stuff except2, final String message) {
		for(Stuff s : this) {
			if(s == except1 || s == except2) continue;
			s.sendTo(message);
		}
	}

	public final void sendToContents(final String message) {
		/* fixme: perhaps make this recursive? pros: if you are on an emu in a
		 tank, this will work properly; cons: a lot of sendTo(inventory);
		 perhaps make a sendToContentsRec()? */
		for(Stuff s : this) s.sendTo(message);
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

	/** @return	Iterate over the contents. */
	public final Iterator<Stuff> iterator() {
		return contents.iterator();
	}

	/* @return	Null since there is no directions (overwriten in Room.) */
	public Room getRoom(Direction dir) { return null; }

	/* @return	Recursive room search; in practice, a depth of one or two, but
	 allows in an in an in, etc; why that would be useful is unclear. */
	public Room getRoom() { return in.getRoom(); }

	/** Sets the flags from a line
	 @param line	The line. */
	public void setFlags(final String line) {
	}

	public boolean isTransportable() {
		return false;
	}

	/** Stuff can not move anywhere. */
	public void setNextDir(final Room.Direction where) { }

	public final void go(final Room.Direction where) {
		if(in == null) {
			sendTo("Can't do that; you are floating in space.");
			return;
		}
		if(!(in instanceof Room)) {
			/* we are mounted somehow */
			in.setNextDir(where);
			in.getHandler().register(in);
			System.err.format("%s.getHandler().register(%s);\n", this, in);
			sendTo("You tell " + /*fixme:an*/in + " to go " + where + ".");
			sendToRoom(this + " tells " + /*the()*/in + " to go " + where + ".");
			return;
		}
		/* we are walking normaly */
		Room target = in.getRoom(where);
		if(target == null) {
			sendTo("You can't go that way.");
			sendToRoom(/*the*/this + " searches for a way " + where + ".");
			return;
		}
		sendTo("You walk " + where + ".");
		sendToRoom(/*The(*/this + " walks " + where + ".");
		placeIn(target);
		sendToRoom(/*The(*/this + " walks in from the " + where.getBack() + ".");
		/* newline -- players have swiched locations */
		sendTo("\n" + target.lookDetailed(this));
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

	public void levelUp() {
		sendTo("Only players can level up.");
	}

	/** Prints all the data so it will be serialisable (but in text, not binary.)
	 @return Blank string; fixme: the serialised version. */
	public String saveString() {
		return ""; //<------
	}

	/**
	 @param test	A function taking in Stuff and outputing true/false.
	 @return		None of the things in contents that it tested were true. */
	public boolean isAll(Predicate<Stuff> test) {
		for(Stuff s : this) {
			/* fixme: recurse on isEnterable() */
			if(!test.test(s)) return false;
		}
		return true;
	}

}
