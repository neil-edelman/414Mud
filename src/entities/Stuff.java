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
import java.util.function.Consumer;

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

	/** @param container	Silently places this in container with no checks on
							whether it can go into that container. If you
							voilate the acycinicness of the world, it will just
							refuse to put it in. */
	public void placeIn(Stuff container) {

		//System.err.format("%s.placeIn(%s);\n", this, container);

		/* check that we're still a DAG (viz, tree;) ie, you can't move
		 something inside itself */
		if(this == container || !isAllContent((s) -> s != container)) {
			System.err.format("%s.placeIn(%s): no (doing so would voilate acyclic condition.)", this, container);
			return;
		}

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
		System.err.format("%s.hasMoved()\n", this);
		forAllContent((stuff) -> stuff instanceof Player, (player) -> player.hasMoved());
	}

	/** @return		The one above it in the Stuff tree. Can be null. */
	public final Stuff getIn() {
		return in;
	}

	/** Overwrote on things that have a connection; viz, Player.
	 @param message		The string to send; no newline. */
	public void sendTo(final String message) {
	}

	public final void sendToRoom(final String message) {
		Room r;
		if((r = getRoom()) == null) return;
		r.sendToContentsExcept(this, message);
	}

	public final void sendToRoomExcept(final Stuff except, final String message) {
		Room r;
		if((r = getRoom()) == null) return;
		r.sendToContentsExceptTwo(this, except, message);
	}

	/* fixme: stuffprogs are activated on some event, you have to get rid of
	 (s instanceof Player) (yes; tank !instanceof Player, skip it, even though
	 player was inside; which was not what we wanted from culling) -- forget it */

	public final void sendToContents(final String message) {
		forAllContent((s) -> true,
					  (s) -> s.sendTo(message));
	}

	public final void sendToContentsExcept(final Stuff except, final String message) {
		forAllContent((s) -> (s != except),
					  (s) -> s.sendTo(message));
	}

	public final void sendToContentsExceptTwo(final Stuff except1, final Stuff except2, final String message) {
		forAllContent((s) -> (s != except1) && (s != except2),
					  (s) -> s.sendTo(message));
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
	public Room getRoom() { return (in == null) ? null : in.getRoom(); }

	/** Sets the flags from a line. There are no flags on Stuff.
	 @param line	The line. */
	public void setFlags(final String line) { }

	/** @return	Stuff is not transportable. */
	public boolean isTransportable() { return false; }

	/** Stuff can not move anywhere. */
	public void setNextDir(final Room.Direction where) { }

	/** @param where	Direction in which this moves. */
	public final void go(final Room.Direction where) {
		if(in == null) {
			sendTo("Can't do that; you are floating in space.");
			return;
		}
		/* we are mounted somehow? */
		if(!(in instanceof Room)) {
			in.setNextDir(where);
			in.getHandler().register(in);
			//System.err.format("%s.getHandler().register(%s);\n", this, in);
			sendTo("You tell " + /*fixme:an*/in + " to go " + where + ".");
			sendToRoom(/*the(*/this + " tells " + /*the()*/in + " to go " + where + ".");
			return;
		}
		/* we are walking normaly */
		Room target = in.getRoom(where);
		if(target == null) {
			sendTo("You can't go " + where + ".");
			sendToRoom(/*the*/this + " searches for a way " + where + ".");
			sendToContents("You can't go " + where + " on " + this + ".");
			return;
		}
		Room r;
		
		sendTo(/*ride?*/"You walk " + where + ".");
		sendToRoom(/*The(*/this + /*this.walking*/" walks " + where + ".");
		placeIn(target);
		sendToRoom(/*The(*/this + /*this.walking*/" walks in from the " + where.getBack() + ".");
		/* newline -- players have swiched locations */
		sendTo("\n" + target.lookDetailed(this/*do we want to see the mount? no*/)); /* newline! */
		/* send to contents, too! */
		StringBuilder sb = new StringBuilder("You ride ");
		sb.append(where);
		sb.append(" on ");
		sb.append(this.toString());
		sb.append(".\n\n");
		sb.append(target.lookDetailed(this));
		sendToContents(sb.toString());
	}

	/** fixme: An/an(aeiou) The/the(isCapital()?/isProper) */

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
	}

	public void levelUp() {
		sendTo("Only players can level up.");
	}

	/** Prints all the data so it will be serialisable (but in text, not binary.)
	 @return Blank string; fixme: the serialised version. */
	public String saveString() {
		return ""; //<------ fixme
	}

	/** Wake up (viz, when a Player is in the area.) */
	public final void wakeUp() {
		getHandler().register(this);
		System.err.format("%s is awake!\n", this);
	}

	/** A recusive function short-circuit testing if everything is true for all
	 the contents.
	 @param test	A function taking in Stuff and outputing true/false.
	 @return		None of the things in contents that it tested were true. */
	public final boolean isAllContent(final Predicate<Stuff> test) {
		for(Stuff s : this) {
			//System.err.format("(Stuff)%s? ", s);
			/* recurse as needed */
			if(!test.test(s) || (isEnterable() && !s.isAllContent(test))) return false;
		}
		return true;
	}

	/** Stuff in content recursively will be called with test and, if true, set.
	 @param test	A function which which determines if set should be called.
	 @param set		A function which will be called if test is true. */
	public final void forAllContent(final Predicate<Stuff> test, final Consumer<Stuff> set) {
		for(Stuff s : this) {
			if(!test.test(s)) continue;
			set.accept(s);
			/* recurse */
			if(isEnterable()) s.forAllContent(test, set);
		}
	}

}
