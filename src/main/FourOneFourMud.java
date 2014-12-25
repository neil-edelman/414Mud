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
//import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import java.text.ParseException;
import common.TextReader;

import java.util.Map;
import java.util.HashMap;

import entities.Room;
import entities.Object;
import entities.Mob;
import main.Connection;
import main.Area;

import main.Tests;

/** This is the entry-point for starting the mud and listening for connections;
 connnections are handled by a fixed socket pool.

 @author	Neil
 @version	1.1, 12-2014
 @since		1.0, 11-2014 */
public class FourOneFourMud implements Iterable<Connection> {

	/* debug mode; everyone can read this */
	public static boolean isVerbose = true;

	/* constants */
	private static final int fibonacci20    = 6765;
	private static final int sStartupDelay  = 20;
	private static final int sShutdownTime  = 10;
	private static final int sPeriod        = 10;
	private static final String dataDir     = "data";
	private static final String areasDir    = dataDir + "/areas";
	private static final String mudData     = "mud";

	/** Starts up the mud and listens for connections.
	 @param args	Ignored. */
	public static void main(String args[]) {

		try {
			Tests t = new Tests();
			System.err.format("%s\n\n\n", t);
		} catch(IOException e) {
			System.err.format("%s\n", e);
		}

		FourOneFourMud mud;

		/* run mud */

		try {
			mud = new FourOneFourMud(dataDir, mudData);
		} catch(IOException e) {
			System.err.format("Connection wouldn't complete: %s.\n", e);
			/* deal-breaker */
			return;
		}

		mud.run();

		mud.shutdown();

	}

	/* the thread that is scheduleAtFixedRate */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final ScheduledFuture<?> chonosFuture;
	private final Runnable chonos = new Runnable() {
		/* one time step */
		public void run() {
			System.out.print("<Bump>\n");
		}
	};

	/* the clients of the mud */
	private final ServerSocket    serverSocket;
	private final int             poolSize;
	private final ExecutorService pool;
	private List<Connection>      clients = new LinkedList<Connection>();

	/* the mud data */
	private String name     = "414Mud";
	private int    port     = fibonacci20;
	private String password = "";
	private String motd     = "Hello.";
	private Map<String, Area> areas = new HashMap<String, Area>();
	private Area   homearea;
	private Room   homeroom;

	/** The entire mud constructor.
	 @param dataDir			The subdirectory where the data file is located.
	 @param mudData			The data file name.
	 @throws IOException	Passes the IOException from the underlyieng sockets. */
	public FourOneFourMud(final String dataDir, final String mudData) throws IOException {
		String homeareaStr  = "";
		int    homeareaLine = -1;
		int    poolSize     = 256;

		/* read in settings */

		Path path = FileSystems.getDefault().getPath(dataDir, mudData);
		try(TextReader text = new TextReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
			name         = text.nextLine();
			port         = text.nextInt();
			poolSize     = text.nextInt();
			homeareaStr  = text.readLine();
			homeareaLine = text.getLineNumber();
			password     = text.nextLine();
			motd         = text.nextParagraph();
		} catch(ParseException e) {
			System.err.format("%s/%s; syntax error: %s, line %d.\n", dataDir, mudData, e.getMessage(), e.getErrorOffset());
			throw new IOException(dataDir + "/" + mudData);
		}
		/* final = rewritable */
		this.poolSize = poolSize;
		System.err.format("%s: port %d, max connections %d, home area <%s>.\n", this, port, poolSize, homeareaStr);
		System.err.format("%s: set the ascent password: <%s>.\n", this, password);
		System.err.format("%s: set MOTD: <%s>.\n", this, motd);

		/* read in areas */

		areas = Area.loadAreas(areasDir);

		/* set the [defaut] recall spot */

		homearea = areas.get(homeareaStr);
		try {
			if(homearea == null) throw new Exception("area <" + homeareaStr + "> (line " + homeareaLine + ") does not exist; connections will be sent to the null room");
			homeroom = homearea.getRecall();
			System.err.format("%s: set home room: <%s.%s>.\n", this, homearea, homeroom);
		} catch(Exception e) {
			System.err.format("%s/%s: %s.\n", dataDir, mudData, e.getMessage());
			/* we let is start anyway; it's a chat server at least */
		}

		/* start the networking */

		serverSocket = new ServerSocket(port);
		pool         = Executors.newFixedThreadPool(poolSize);

		/* start the timer */

		System.err.format("%s: starting timer.\n", this);
		chonosFuture = scheduler.scheduleAtFixedRate(chonos, sStartupDelay, sPeriod, TimeUnit.SECONDS);

	}

	/** Run the mud. */
	private void run() {
		try {
			/* wait for incoming connections */
			for( ; ; ) {
				Connection client = new Connection(serverSocket.accept(), this);
				clients.add(client);
				pool.execute(client);
			}
		} catch(SocketException e) {
			/* this occurs if the serverSocket is closed */
			System.err.format("%s: shutting down.\n", this);
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

	/** Closes the server; it will detect this through an exception, and shutdown. */
	public void shutdown() {
		System.err.format("%s: stopping timer.\n", this);
		chonosFuture.cancel(false);
		scheduler.shutdown();
		try {
			if(!scheduler.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
				if(!scheduler.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
					System.err.print("The timer would not terminate.\n");
				}
			}
		} catch (InterruptedException e) {
			System.err.print(this + ": terminating timer.");
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
		System.err.format("%s: shutting down server socket.\n", name);
		try {
			serverSocket.close();
		} catch(IOException e) {
			System.err.format("%s: badness. %s.\n", name, e);
		}

		System.err.format("%s is shutdown.\n", name);

	}

	/** Prints out the mud info. */
	public String toString() {
		return name;
	}

	/** @return The place you start. */
	public Room getHome() {
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
