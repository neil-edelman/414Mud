/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main /*.handlers fuck you "package does not exist," yes, that's why I'm
			  definining it; I don't understand */;

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
import java.util.NoSuchElementException;

import java.util.regex.Pattern;

import common.BoundedReader;
import common.Orcish;
import entities.Character;
import entities.Player;
import entities.Stuff;

/** Connections are the people connected to our mud; later we will build a
 character around them and put them in the game.
 <p>
 Connections are Runnable things; the FixedThreadPool socket pool assigns
 serverSocket.accept() which is passed to the constructor. Boolean isExit is
 checked at every line, or we could force exit though a SocketException (eg,
 mud shutdown.)
 
 @author	Neil
 @version	1.1, 2014-12
 @since		1.0, 2014-11 */
public class Connection implements Mud.Handler {

	private static final String newbieCommandset = "newbie";

	private static final int     bufferSize     = 80 * 2;
	private static final String  cancelCommand  = "~";	
	/* ^[\x00-\x1F\x7F] */
	private static final Pattern invalidPattern = Pattern.compile("\\p{Cntrl}");

	private final Socket socket;
	private final String name = Orcish.get();
	private final Mud    mud;

	private Map<String, Command> commands;
	private boolean         isWaiting;
	private PrintWriter     out;
	private BoundedReader   in;
	/*private OutputStream    outRaw = null;
	private InputStream     inRaw = null; <- for ansi commands */
	private Player  player;
	private boolean isExit = false;
	/* so we don't have to worry about synconisation, we'll assign each
	 Thread/Socket/Connection one and operate them in parallel */
	private Mapper          mapper = new Mapper();
	
	/** Initalize the connection.
	 @param socket	The client socket. */
	Connection(final Socket socket, final Mud mud) {
		this.socket = socket;
		this.mud    = mud;
		this.player = new Player(this);
		try {
			this.commands = mud.getCommands(newbieCommandset);
		} catch(NoSuchElementException e) {
			System.err.format("%s: not loaded <%s>.\n", this, newbieCommandset);
			sendTo("No command set '" + newbieCommandset + ".'");
		}
		System.err.print(this + " has connected to " + mud + ".\n");
	}

	/* for Mud.Handler, the thead thing */
	public Mud    getMud()                    { return mud; }
	public Map<String, Command> getCommands() { return commands; };
	public void setCommands(final String cmdStr) throws NoSuchElementException {
		commands = getMud().getCommands(cmdStr);
	}
	public Mapper getMapper()                 { return mapper; }
	public void   setExit()                   { isExit = true; }
	public void   register(Stuff stuff) {
		if(!(stuff instanceof Player)) {
			System.err.format("%s: that's funny, <%s>, is not a Player; ignoring.\n", this, stuff);
			return;
		}
		//mud.players . . . fixme! we don't have to keep track of players in mud
		//players.add((Player)stuff);
		System.err.format("%s: registered <%s>. (but not really)\n", this, stuff);
	}
	/** The server-side handler for connections. */
	public void run() {
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
				sendToRaw(player.getPrompt());
				isWaiting = true;
				if((input = in.readLine()) == null) break;
				isWaiting = false;

				/* if the string is sanitary, interpret it */
				if(!invalidPattern.matcher(input).find()) {
					interpret(input);
				} else {
					this.sendTo("Weird characters within your input; ignoring.");
				}

			}

			this.sendTo("Closing " + this + ".");
			mud.deleteClient(this);
			socket.close();

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

	/** @return		The client socket. */
	public Socket getSocket() {
		return socket;
	}

	/** @return		Gets the player assocated with this connection. */
	public Player getPlayer() {
		return player;
	}

	/** @param p	Sets the player to another player, used at character creation. */
	public void setPlayer(Player p) {
		player = p;
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
		Command command = commands.get(cmd);

		if(command != null) {
			//command.command(this.playerlike, arg);
			command.command(this.player, arg);
		} else {
			sendTo("Huh? \"" + cmd + "\" (use help for a list)");
		}
	}

	/** Send a message to the connection.
	 @param message		The message. */
	public void sendTo(final String message) {

		//System.err.print("Sending " + this + ": " + message + "\n");
		if(in == null) return;

		StringBuilder sb = new StringBuilder();
		if(isWaiting) sb.append("\n"); // fixme: it works, but should really be newLine(), \r\n, right?
		sb.append(message);
		sb.append("\n");
		if(isWaiting) sb.append(player.getPrompt());
		sendToRaw(sb.toString());
	}
	/** Send a message to the connection without prompt or newlines
	 @param message		The message. */
	private void sendToRaw(final String message) {
		if(out == null) return;
		out.print(message);
		out.flush();
	}

	public String toString() {
		String s = "Connection " + name + "(" + player + ")";
		return s;
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

	/** This is not used . . . yet. It's much trickier to send a binary command
	 over the internet. */
	private enum Telnet {
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

}
