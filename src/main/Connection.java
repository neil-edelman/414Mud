/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

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
import java.lang.NoSuchFieldException;

import java.util.regex.Pattern;

import common.BoundedReader;
import common.Orcish;
import entities.Player;

/* 0 black
 * 1 red
 * 2 green
 * 3 yellow
 * 4 blue
 * 5 magenta
 * 6 cyan
 * 7 white */

/* (n)F - Moves cursor to beginning of the line n (default 1) lines up.
 * (n)L - Insert Line, current line moves down.
 * (n)S - Scroll up, entire display is moved up, new lines at bottom
 * (n)E - Cursor to Next Line. If the active position is at the bottom margin,
 *        a scroll up is performed. */

/** Connections are the people connected to our mud; later we will build a
 character around them and put them in the game.
 <p>
 Connections are Runnable things; the FixedThreadPool socket pool assigns
 serverSocket.accept() which is passed to the constructor. Boolean isExit is
 checked at every line, or we could force exit though a SocketException.
 
 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public class Connection extends Commandset implements Runnable {

	enum Telnet {
		NAWS(31),	// Negotiate About Window Size
		SE(240),	// End of subnegotiation parameters.
		NOP(241),	// No operation
		DM(242),	// Data mark. Should TCP Urgent.
		BRK(243),	// Indicates that the "break" key was hit.
		IP(244),	// Suspend, interrupt or abort.
		AO(245),	// Abort output.
		AYT(246),	// Are you there?
		EC(247),	// Erase character.
		EL(248),	// Erase line.
		GA(249),	// Go ahead.
		SB(250),	// Subnegotiation of the indicated option follows.
		WILL(251),	// Indicates the desire to begin performing, or confirmation that you are now performing.
		WONT(252),	// Indicates the refusal to perform, or continue performing
		DO(253),	// Indicates the request that the other party perform, or confirmation expecting
		DONT(254),	// Indicates the demand that the other party stop performing.
		IAC(255);	// Interpret as command
		private int command;
		Telnet(final int command)	{ this.command = command; }
		int val()					{ return command; }
		byte bval()                 { return (byte)command; }
	}

	private static final int bufferSize       = 80;
	private static final String cancelCommand = "~";	

	/* ^[\x00-\x1F\x7F] */
	private static final Pattern invalidPattern = Pattern.compile("\\p{Cntrl}");

	private final Socket socket;
	private final String name = Orcish.get();
	private final FourOneFourMud mud;

	private Map<String, Command> commandset = null;
	private boolean         isWaiting;
	private PrintWriter     out;
	private BoundedReader   in;
	/*private OutputStream    outRaw = null;
	private InputStream     inRaw = null;*/
	private Player  player = null;
	private boolean isExit = false;
	/* fixme: ip */

	/** Initalize the connection.
	 @param socket	The client socket. */
	Connection(final Socket socket, final FourOneFourMud mud) {
		this.socket     = socket;
		this.mud        = mud;
		try {
			setCommandset("newbie");
		} catch(NoSuchFieldException e) {
			System.err.format("%s: %s.\n", this, e);
		}
		System.err.print(this + " has connected to " + mud + ".\n");
	}

	/** The server-side handler for connections. */
	public void run() {
		//System.err.print(this + " up and running, waiting for character creation.\n");
		try(
			PrintWriter   out = new PrintWriter(/*outRaw = */socket.getOutputStream(), true /* autoflush (doesn't work?) */);
			BoundedReader  in = new BoundedReader(new BufferedReader(new InputStreamReader(/*inRaw = */socket.getInputStream())), bufferSize);
		) {
			String input;

			/* make these class variables so others can talk to us using
			 sentTo(), getFrom() */
			this.out = out;
			this.in  = in;

			/* first thing -- get the screen width */
			/*System.err.format("%s: sending request for screen width.\n", this);
			try{
				doNaws();
			} catch(IOException e) {
				System.err.format("%s: %s.\n", this, e);
			}*/

			System.err.print("Sending MOTD to " + this + ".\n");
			this.sendTo(mud.getMotd());
			this.sendTo(mud + ": you are " + this + "; type 'create <Character>' to start.");

			while(!isExit) {

				/* wait for next line */
				isWaiting = true;
				if((input = in.readLine()) == null) break;
				isWaiting = false;

				/* if the string is sanitary, interpret it */
				if(!invalidPattern.matcher(input).find()) {
					interpret(input);
				} else {
					this.sendTo("Weird characters within your input; ignoring.");
				}

				/* do the prompt again */
				if(player != null) sendToRaw(player.prompt());

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
			this.out    = null;
			this.in     = null;
			/*this.outRaw = null;
			this.inRaw  = null;*/
		}

	}

	/** Send a message to the connection.
	 @param message		The message. */
	public void sendTo(final String message) {

		//System.err.print("Sending " + this + ": " + message + "\n");
		if(in == null) return;

		StringBuilder sb = new StringBuilder();
		if(isWaiting) sb.append("\n"); // fixme: it works, but should really be newLine(), \r\n
		sb.append(message);
		sb.append("\n");
		if(isWaiting && player != null) sb.append(player.prompt());
		sendToRaw(sb.toString());
	}

	private void sendToRaw(final String message) {
		if(out == null) return;
		out.print(message);
		out.flush();
	}

/*	private boolean expectIn(final byte[] data) throws IOException {
		if(inRaw == null) return false;
		boolean dont = false;
		int     b;
		for(byte d : data) {
			b = inRaw.read();
			if(b != (int)(d & 0xff)) dont = true;
		}
		return !dont;
	}

	private boolean doNaws() throws IOException {
		int iac, sb, naws, b1, b2;

		if(outRaw == null || inRaw == null) return false;

		* IAC DO NAWS
		 Sent by the Telnet server to suggest that NAWS be used. *
		byte[] yo = new byte[] { Telnet.IAC.bval(), Telnet.DO.bval(), Telnet.NAWS.bval() };
		* IAC WILL NAWS
		Sent by the Telnet client to suggest that NAWS be used.*
		byte[] ok = new byte[] { Telnet.IAC.bval(), Telnet.WILL.bval(), Telnet.NAWS.bval() };

		outRaw.write(yo);
		outRaw.flush();
		
		return expectIn(ok);
	}

		iac = inRaw.read();
		if(iac != Telnet.IAC.val()) throw new IOException("read " + iac + " expecting IAC");

		sb = inRaw.read();
		if(sb != Telnet.DONT.val() &&
		   sb != Telnet.SB.val()) throw new IOException("read " + sb + " when expecting SB");

		naws = inRaw.read();
		if(naws != Telnet.NAWS.val()) throw new IOException("read " + naws + " when expecting NAWS");

		if(sb == Telnet.DONT.val()
		   && naws == Telnet.NAWS.val()) throw new IOException("client refuses to send window size");

		b2 = inRaw.read();
		b1 = inRaw.read();
		if(b2 == -1 || b1 == -1) throw new IOException("dubious window size");
		int width = (b2 << 8) | b1;
		b2 = inRaw.read();
		b1 = inRaw.read();
		if(b2 == -1 || b1 == -1) throw new IOException("dubious window size");
		int height = (b2 << 8) | b1;
		System.err.format("Width %d Height %d?\n", width, height);
		//....
	}
*/
		/* IAC DON'T NAWS
		 Sent by the Telnet server to refuse to use NAWS. */
		/* IAC SB NAWS <16-bit value> <16-bit value> IAC SE
		 Sent by the Telnet client to inform the Telnet server of the
		 window width and height. */

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

	public Map<String, Command> getCommandset() {
		return commandset;
	}

	public void setCommandset(final String commandsetStr) throws NoSuchFieldException {
		Map<String, Command> c = commandsets.get(commandsetStr);
		if(c == null) throw new NoSuchFieldException(commandsetStr + " not found");
		commandset = c;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player p) {
		player = p;
	}

	public void setExit() {
		isExit = true;
	}

	/* @depreciated	Not used. */
	public void sendToRoom(final String s) {
		if(player == null) return;
		player.sendToRoom(s);
	}

	/** This parses the string and runs it.
	 @param c		The connection that's attributed the command.
	 @param command	A command to parse. */
	public void interpret(final String commandStr) {
		String cmd, arg;

		/* break it off if ~ is at the end */
		if(commandStr.endsWith(cancelCommand)) {
			sendTo("Cancelled.");
			return;
		}
		/* break the string up */
		/* fixme: any white space */
		int space = commandStr.indexOf(' ');
		if(space != -1) {
			cmd = commandStr.substring(0, space);
			arg = commandStr.substring(space).trim();
		} else {
			cmd = commandStr;
			arg = "";
		}

		/* parse */
		Command command = commandset.get(cmd);

		if(command != null) {
			command.command(this, arg);
		} else {
			sendTo("Huh? \"" + cmd + "\" (use help for a list)");
		}
	}

}
