/* Copyright 2014 Sid Gandhi and Neil Edelman, distributed under the terms of
 the GNU General Public License, see copying.txt */

package main;

import java.net.ServerSocket;
import java.net.SocketException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

//import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import entities.Room;
import entities.Object;
import entities.Mob;
import main.Connection;
import main.Area;

/** This is the entry-point for starting the mud and listening for connections;
 connnections are handled by a fixed socket pool.

 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public class FourOneFourMud implements Iterable<Connection> {

	private static final int fibonacci20    = 6765;
	private static final int sStartupDelay  = 2;
	private static final int sShutdownTime  = 10;
	private static final int sPeriod        = 3;
	private static final String dataDir     = "data";
	private static final String areasDir    = dataDir + "/areas";
	private static final String mudData     = "mud";

	private static final Runnable chonos = new Runnable() {
		/* one time step */
		public void run() {
			System.out.print("<Bump>\n");
		}
	};

	/* everyone can read this */
	public static boolean isVerbose = true;

	private static String name        = "414Mud";
	private static String homeareaStr = "";
	private static String password    = "";
	private static String motd        = "Hello.";
	private static Area   homearea;
	private static Room   homeroom;

	/** Starts up the mud and listens for connections.
	 @param args	Ignored. */
	public static void main(String args[]) {
		int port = fibonacci20, maxConnections = 256;

		/* read in settings */

		int homeareaLine = 0;
		Path path = FileSystems.getDefault().getPath(dataDir, mudData);
		try(LineNumberReader reader = new LineNumberReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
			String line;
			if((line = reader.readLine()) != null) name        = line;
			if((line = reader.readLine()) != null) homeareaStr = line;
			homeareaLine = reader.getLineNumber();
			if((line = reader.readLine()) != null) port        = Integer.parseInt(line);
			if((line = reader.readLine()) != null) maxConnections = Integer.parseInt(line);
			if((line = reader.readLine()) != null) password    = line;
			if((line = reader.readLine()) != null) motd        = line;
		} catch(IOException e) {
			System.err.format("IOException: %s.\n", e);
		}

		System.err.print("Set MUD: <" + name + " : " + port + ">.\n");
		System.err.print("Set max connections: " + maxConnections + ".\n");
		System.err.print("Set the secret password for becoming an Immortal: <" + password + ">.\n");
		System.err.print("Set MOTD: <" + motd + ">.\n");

		/* read in areas */

		Area.loadAreas(areasDir);

		/* set the [defaut] recall spot */

		homearea = Area.getArea(homeareaStr);
		try {
			if(homearea == null) throw new Exception("area <" + homeareaStr + "> (line " + homeareaLine + ") does not exist; connections will be sent to the null room");
			homeroom = homearea.getRecall();
			System.err.format("Set home room: <%s.%s>.\n", homearea, homeroom);
		} catch(Exception e) {
			System.err.format("%s/%s: %s.\n", dataDir, mudData, e.getMessage());
			/* we let is start anyway; it's a chat server at least */
			//return;
		}

		/* run mud */

		FourOneFourMud mud;

		try {
			mud = new FourOneFourMud(port, maxConnections);
		} catch(IOException e) {
			System.err.format("Connection wouldn't complete: %s.\n", e);
			/* deal-breaker */
			return;
		}

		/* start the timer */
		System.err.print("Starting timer.\n");
		ScheduledFuture<?> future = mud.timer.scheduleAtFixedRate(chonos, sStartupDelay, sPeriod, TimeUnit.SECONDS);

		mud.run();

		System.err.print("Stopping timer.\n");
		future.cancel(false);
		mud.timer.shutdown();
		try {
			if(!mud.timer.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
				mud.timer.shutdownNow();
				if(!mud.timer.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
					System.err.print("The timer would not terminate.\n");
				}
			}
		} catch (InterruptedException e) {
			System.err.print("Terminating timer.");
			mud.timer.shutdownNow();
			Thread.currentThread().interrupt();
		}

		mud.shutdown();
		System.err.format("%s is shutdown.\n", name);

	}

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

	private final ServerSocket    serverSocket;
	private final ExecutorService pool;

	private List<Connection> clients = new LinkedList<Connection>();

	/** The entire mud constructor.
	 @param port			The mud port.
	 @param poolSize		How many simultaneous connections should we allow.
	 @throws IOException	Passes the IOException from the underlyieng sockets. */
	public FourOneFourMud(int port, int poolSize) throws IOException {
		System.err.print("414Mud starting up on port " + port
						 + "; FixedThreadPool size " + poolSize + ".\n");
		serverSocket = new ServerSocket(port);
		pool         = Executors.newFixedThreadPool(poolSize);
	}

	/** Run the mud. */
	private void run() {
		/* fixme: how to get try-with-resorces to work? */
		try {
			for( ; ; ) {
				/* fixme! immortal -> newbie (makes testing difficult) */
				Connection client = new Connection(serverSocket.accept(), this);
				clients.add(client);
				pool.execute(client);
			}
		} catch(SocketException e) {
			/* this occurs if the serverSocket is closed; yes, this is how we
			 shut it down :[ */
			System.err.format("%s shutting down.\n", this);
		} catch(IOException e) {
			System.err.format("Shutting down: %s.\n", e);
		} finally {
			/* reject incoming tasks */
			pool.shutdown();
			try {
				System.err.print("Waiting " + sShutdownTime + "s for clients to terminate.\n");
				if(!pool.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
					System.err.print("Terminating clients " + sShutdownTime + "s.\n");
					pool.shutdownNow();
					if(!pool.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
						System.err.print("A clients did not terminate.\n");
					}
				}
				System.err.print("Server socket closing.\n");
				serverSocket.close(); // fixme: autoclosable, will already be closed in most sit
			} catch(InterruptedException ie) {
				// (Re-)Cancel if current thread also interrupted
				pool.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			} catch(IOException e) {
				System.err.format("Server socket error. %s.\n", e);
			}
		}

	}

	/** Closes a connection.
	 @param c The connection to close. */
	public void deleteClient(Connection c) {
		System.err.print(c + " is closed: " + c.getSocket().isClosed() + "; removing from " + name + ".\n");
		clients.remove(c);
	}

	/** Closes the server; it will detect this, and shutdown. */
	public void shutdown() {
		try {
			serverSocket.close();
		} catch(IOException e) {
			System.err.format("%s::shutdown: badness. %s.\n", name, e);
		}
	}

	/** Prints out the mud info. */
	public String toString() {
		return name;
	}

	/** @return The place you start. */
	public Room getUniverse() {
		return homeroom;
	}

	/** @param p	The test password.
	 @return		True is the password matches the one when the mud started up. */
	public boolean comparePassword(final String p) {
		return p.compareTo(password) == 0;
	}

	/** @return	Gets the iterator of all the connections. */
	public Iterator<Connection> iterator() {
		return clients.iterator();
	}

	/** @return	Gets the Message of the Day. */
	public String getMotd() {
		return motd;
	}

}
