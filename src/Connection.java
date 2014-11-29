import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import java.io.OutputStreamWriter;

/** Connections are the people connected to our mud; later we will build a
 character around them and put them in the game.
 @author Neil */

class Connection implements Runnable {

	private final Socket socket;
	private final String name = Orcish.get();
	private final FourOneFourMud mud;
	private PrintWriter   out;
	private BufferedReader in;
	/* fixme: ip */

	/** Initalize the connection.
	 @param socket
		the client socket */
	Connection(final Socket socket, final FourOneFourMud mud) {
		System.err.print(this + " initialising.\n");
		this.socket = socket;
		this.mud    = mud;
	}

	/** The server-side handler for connections. */
	public void run() {
		System.err.print(this + " up and running, waiting for character creation.\n");
		/* autoflush */
		try(
			PrintWriter   out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		) {
			this.out = out;
			this.in  = in;

			String input, foo = "foo";

			//System.err.print("Sending \"" + foo + "\" to " + this + ".\n");
			this.sendTo("You are " + this + "; " + foo + ".\n");
			while((input = this.getFrom()) != null) {
				if(input.length() == 0) break;
				System.out.print(this + " sent \"" + input + ".\"\n");
				this.sendTo(this + " sent \"" + input + ".\"\n");
				if(input.compareToIgnoreCase("shutdown") == 0) {
					mud.shutdown();
					break;
				}
			}
			out.print("10-4 over and out.\n");
			out.flush();
			System.err.print("Closing " + this + ".\n");
		} catch(UnsupportedEncodingException e) {
			System.err.print(this + " doesn't like UTF-8 " + e + ".\n");
		} catch(IOException e) {
			System.err.print(this + " " + e + ".\n");
		} finally {
			this.out = null;
			this.in  = null;
		}
	}

	/** Send a message to the connection.
	 @param message
		The message. */
	public void sendTo(final String message) {
		/* "telnet newline sequence \r\n" <- or this? */
		if(out == null) return;
		out.print(message);
		out.flush();
		System.err.print("Sending " + this + ": " + message);
	}

	/** Wait for a message from the connection.
	 @return The message. */
	public String getFrom() throws IOException {
		if(in == null) return null;
		return in.readLine();
	}

	public String toString() {
		return "Connection " + name;
	}

}
