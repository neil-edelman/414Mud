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
	/* fixme: ip */

	/** Initalize the connection.
	 @param socket
		the client socket */
	Connection(Socket socket, FourOneFourMud mud) {
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
			String input, foo = "foo";

			System.err.print("Sending \"" + foo + "\" to " + this + ".\n");
			out.print("You are " + this + "; " + foo + ".\n");
			out.flush(); /* <- important */
			/* "telnet newline sequence \r\n" <- or this? */
			while((input = in.readLine()) != null) {
				if(input.length() == 0) break;
				System.out.print(this + " sent \"" + input + ".\"\n");
				out.print(this + " sent \"" + input + ".\"\n");
				out.flush();
				/* oops! fixme: you always call shutdown to free up sockets!
				 this should be out of the loop, mud shutdown is handled in
				 414Mud::run */
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
		}
	}

	public String toString() {
		return "Connection " + name;
	}

}
