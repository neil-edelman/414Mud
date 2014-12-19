/** Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt
 
 Connections are Runnable things; the FixedThreadPool socket pool assigns
 serverSocket.accept() which is passed to the constructor. Boolean isExit is
 checked at every line, or we could force exit though a SocketException.
 
 @author Neil
 @version 1.1
 @since 2014 */

package main;

import java.net.Socket;
import java.net.SocketException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

import main.BoundedReader;
import entities.Player;

/** Connections are the people connected to our mud; later we will build a
 character around them and put them in the game.
 @author Neil */

public class Connection implements Runnable {

	private static final Commandset newbie   = new Commandset(Commandset.Level.NEWBIE);
	private static final Commandset common   = new Commandset(Commandset.Level.COMMON);
	private static final Commandset immortal = new Commandset(Commandset.Level.IMMORTAL);
	private static final int bufferSize = 80;

	private final Socket socket;
	private final String name = Orcish.get();
	private final FourOneFourMud mud;
	private Commandset commands;
	private PrintWriter     out;
	private BoundedReader   in;
	private Player  player = null;
	private boolean isExit = false;
	/* fixme: ip */

	/** Initalize the connection.
	 @param socket
		the client socket */
	Connection(final Socket socket, final FourOneFourMud mud) {
		this.commands = newbie;
		this.socket   = socket;
		this.mud      = mud;
		System.err.print(this + " has connected to " + mud + ".\n");
	}

	/** The server-side handler for connections. */
	public void run() {
		//System.err.print(this + " up and running, waiting for character creation.\n");
		try(
			PrintWriter   out = new PrintWriter(socket.getOutputStream(), true /* autoflush (doesn't work) */);
			BoundedReader  in = new BoundedReader(new BufferedReader(new InputStreamReader(socket.getInputStream())), bufferSize);
		) {
			String input;

			/* make these class variables so others can talk to us using
			 sentTo(), getFrom() */
			this.out = out;
			this.in  = in;

			System.err.print("Sending MOTD to " + this + ".\n");
			this.sendTo(mud.getMotd());
			this.sendTo(mud + ": you are " + this + "; type 'create <Character>' to start.");

			while(!isExit && (input = in.readLine()) != null) {

				if(input.length() == 0) continue;

				//this.sendTo(this + " sent \"" + input + ".\"");
				commands.interpret(this, input);

			}

			this.sendTo("Closing " + this + ".");
			socket.close();
			mud.deleteClient(this);

		} catch(UnsupportedEncodingException e) {
			System.err.format("%s doesn't like UTF-8: %s.\n", this, e);
		} catch(SocketException e) {
			/* this is usual: input is blocked on the socket, so we close the
			 socket to signal that we're done */
			System.err.format("%s shutting down.\n", this);
		} catch(IOException e) {
			System.err.format("%s: %s.\n", this, e);
		} finally {
			this.out = null;
			this.in  = null;
		}

	}

	/** Send a message to the connection.
	 @param message
		The message. */
	public void sendTo(final String message) {
		if(out == null) return;
		/* I guess Java automatically converts strings to telnet newline \r\n?
		 it works */
		out.print(message + "\n");
		out.flush();
		//System.err.print("Sending " + this + ": " + message + "\n");
	}

	public FourOneFourMud getMud() {
		return mud;
	}

	public String toString() {
		String s = "Connection " + name;
		if(player != null) s += "(" + player + ")";
		return s;
	}

	public String getName() {
		return this.name;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setImmortal() {
		commands = immortal;
	}

	public Commandset getCommandset() {
		return commands;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player p) {
		player = p;
		commands = common;
	}

	public void setExit() {
		isExit = true;
	}

	/* not used */
	public void sendToRoom(final String s) {
		if(player == null) return;
		player.sendToRoom(s);
	}

}
