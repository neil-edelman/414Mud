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
import java.util.Collections;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Scanner;

import java.util.NoSuchElementException;

import entities.Room;
import entities.Object;
import entities.Mob;
import entities.Stuff;
import common.Chance;
import main.Connection;
import main.Chronos;

/** This is the entry-point for starting the mud and listening for connections;
 connnections are handled by a fixed socket pool.

 @author	Neil
 @version	1.1, 2014-12
 @since		1.0, 2014-11 */
public class Mud implements Iterable<Connection> {

	/* debug mode; everyone can read this */
	public static final boolean isVerbose = true;

	/* constants */
	private static final int fibonacci20    = 6765;
	private static final int sStartupDelay  = 20;
	private static final int sShutdownTime  = 10;
	private static final int sPeriod        = 10;
	private static final String dataDir     = "../data";
	private static final String mudData     = "mud";
	private static /*final*/ Mud mudInstance; /* messy! whatever, it keeps us
											   from passing this parameter around */

	/* the mud data */
	private String name     = "414Mud";
	private int    port     = fibonacci20;
	private String password = "";
	private String motd     = "Hello.";
	private Map<String, Area> areas = new HashMap<String, Area>();
	private Area   homearea;
	private Room   homeroom;
	Map<String, Map<String, Command>> commandsets;
	Map<String, Damage> damtypes;

	/* the clients of the mud */
	private final ServerSocket    serverSocket;
	private final int             poolSize;
	private final ExecutorService pool;
	private List<Connection>      clients = new LinkedList<Connection>();

	/* the timer */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final ScheduledFuture<?> chonosFuture;
	private final Chronos chronos;

	/** classes can implement Mud.Loader to be loaded in {@link loadAll}. */
	@FunctionalInterface
	interface Loader<F> { F load(final TextReader in) throws ParseException, IOException; }

	/** Handlers are threads (extends Runnable) that do more stuff. At least
	 they're meant to be. */
	public interface Handler extends Runnable {
		public Mud    getMud();
		public Map<String, Command> getCommands();
		public void setCommands(final String cmdStr) throws NoSuchElementException;
		public Mapper getMapper();
		public Chance getChance();
		public void   setExit();
		public void   register(final Stuff stuff);
	}

	/** The entire mud constructor.
	 @param dataDir			The subdirectory where the data file is located.
	 @param mudData			The data file name.
	 @throws IOException	Passes the IOException from the underlyieng sockets. */
	public Mud(final String dataDir, final String mudData) throws IOException {
		String homeareaStr  = "";
		int    homeareaLine = 0;
		int    poolSize     = 256;

		if(mudInstance != null) throw new RuntimeException("There already is a mud.");
		mudInstance = this;

		//File file = new File(dataDir + "/" + mudData);
		Path path = FileSystems.getDefault().getPath(dataDir, mudData);
		try(TextReader text = new TextReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
			name         = text.nextLine();
			port         = text.nextInt();
			poolSize     = text.nextInt();
			homeareaStr  = text.readLine();
			homeareaLine = text.getLineNumber();
			password     = text.nextLine();
			motd         = text.nextParagraph();
			/* could go really customisable and set the, eg, ".area", but why? */
		} catch(ParseException e) {
			System.err.format("%s/%s; syntax error: %s, line %d.\n",
							  dataDir, mudData, e.getMessage(), e.getErrorOffset());
			throw new IOException(dataDir + "/" + mudData);
		}
		this.poolSize = poolSize; /* set class final = local rewritable */
		System.err.format("%s: port %d, max connections %d, home area <%s>.\n",
						  this, port, poolSize, homeareaStr);
		System.err.format("%s: set the ascent password: <%s>.\n", this, password);
		System.err.format("%s: set MOTD: <%s>.\n", this, motd);


		/* new LoadCommands() is like a lambda, but in it's own file; because
		 it's big */
		commandsets = loadAll("commandsets", ".cset", new LoadCommands());
		damtypes    = loadAll("damagetypes", ".damt", (in) -> new Damage(in));
		/* after commandsets so that stuff.cset is loaded and we don't get a null-pointer-
		 exception, but before the areas are loaded so mobs will be able to register */
		chronos     = new Chronos(this);
		areas       = loadAll("areas",       ".area", (in) -> { return new Area(in); });
		/* set the [defaut] recall spot */
		if((homearea = areas.get(homeareaStr)) != null) {
			System.err.format("%s: set home room: <%s.%s>.\n", this, homearea, homeroom);
			homeroom = homearea.getRecall();
		} else {
			System.err.format("%s/%s: area <%s> (line %d) does not exist; connections will be sent to null.\n",
							  dataDir, mudData, homeareaStr, homeareaLine);
		}

		/* start the networking */

		serverSocket = new ServerSocket(port);
		pool         = Executors.newFixedThreadPool(poolSize);

		/* start the timer */

		System.err.format("%s: starting timer.\n", this);
		chonosFuture = scheduler.scheduleAtFixedRate(chronos, sStartupDelay, sPeriod, TimeUnit.SECONDS);
	}

	/** @return	Gets the iterator of all the connections. Part of Interator interface. */
	public Iterator<Connection> iterator() {
		return clients.iterator();
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
				serverSocket.close(); // fixme: autoclosable, will already be closed in most sit?
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

	/** Starts up the mud and listens for connections.
	 @param args	Ignored. */
	public static void main(String args[]) {
		Mud mud;

		/* run mud */
		try {
			mud = new Mud(dataDir, mudData);
		} catch(IOException e) {
			System.err.format("At top level: %s.\n", e);
			/* deal-breaker */
			return;
		}
		mud.run();
		mud.shutdown();
	}

	/** Closes a connection.
	 @param c The connection to close. */
	public void deleteClient(Connection c) {
		System.err.print(c + " removing from " + name + " (closed: " + c.getSocket().isClosed() + ".)\n");
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

	/****** fixme!!! this is stupid *******/
	public static Mud getMudInstance() {
		return mudInstance;
	}

	public Map<String, Area> getAreas() {
		return areas;
	}

	/** @return The place you start. */
	public Room getHome() {
		return homeroom;
	}

	public Chronos getChronos() {
		return chronos;
	}

	/** @return	Gets the Message of the Day. */
	public String getMotd() {
		return motd;
	}

	/** @return A command set with that name.
	 @throws NamingException	That name isn't loaded. */
	public Map<String, Command>getCommands(final String commandStr) throws NoSuchElementException {
		Map<String, Command> command = commandsets.get(commandStr);
		if(command == null) throw new NoSuchElementException(this
		                     + ": command set <" + commandStr + "> not loaded");
		return command;
	}

	/** @param p	The test password.
	 @return		True is the password matches the one when the mud started up. */
	public boolean comparePassword(final String p) {
		return p.compareTo(password) == 0;
	}

	/** Load all resouces into a hash, one per filename. The hash keys are it's
	 file name minus the extension (F stands for file.)
	 @param dirStr	The directory. */
	static <F> Map<String, F> loadAll(final String dirStr, final String extStr, Loader<F> loader) throws IOException {

		/* make a list of all the data files */
		//File dir = new File(dataDir + "/" + dirStr);
		File dir = FileSystems.getDefault().getPath(dataDir, dirStr).toFile();
		if(!dir.exists() || !dir.isDirectory())
			throw new IOException("<" + dirStr + "> is not a thing");
		File files[] = dir.listFiles(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return name.endsWith(extStr);
			}
		});

		/* go though files, loading each one */
		Map<String, F> loadDir = new HashMap<String, F>();
		for(File file : files) {
			F loadFile = null;
			/* resouce name in the loadDir map is it's filename minus extension */
			String name = file.getName();
			name = name.substring(0, name.length() - extStr.length());
			System.err.format("%s: loading <%s>.\n", name, file);
			try(
				TextReader in = new TextReader(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
				) {
				loadFile = loader.load(in);
			} catch(ParseException e) {
				System.err.format("%s; syntax error: %s, line %d.\n", file,
				                            e.getMessage(), e.getErrorOffset());
			} catch(IOException e) {
				System.err.format("%s; %s.\n", file, e);
			}

			if(loadFile != null) loadDir.put(name, loadFile/*Collections.unmodifiableMap(mod)???*/);

		}

		return loadDir/*Collections.unmodifiableMap(loadMod)???*/;
	}

	/** Prints out the mud info. */
	public String toString() {
		return name;
	}

}
