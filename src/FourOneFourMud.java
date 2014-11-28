import java.net.ServerSocket;
import java.net.SocketException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.LinkedList;

/** This is the entry-point for starting the mud and listening for connections.
 @author Neil */

class FourOneFourMud {

	private static final int fibonacci20    = 6765;
	private static final int maxConnections = 256;
	private static final int sShutdownTime  = 20;

	/** Starts up the mud and listens for connections.
	 @param args
		for future use */
	public static void main(String args[]) {
		try {

			FourOneFourMud mud = new FourOneFourMud(fibonacci20, maxConnections);

			mud.run();

			mud.shutdown();

		} catch (IOException e) {
			/* deal-breaker */
			System.err.print("Connection wouldn't complete: " + e + ".\n");
		}
	}

	private final ServerSocket    serverSocket;
	private final ExecutorService pool;

	private List<Connection> clients;

	/* fixme: whenStarted, name, connected, players, etc . . . */

	/** The entire mud constructor.
	 @param port
		the mud port
	 @param poolSize
		how many simultaneous connections should we allow */
	public FourOneFourMud(int port, int poolSize) throws IOException {
		System.err.print("414Mud starting up.\n");
		serverSocket = new ServerSocket(port);
		pool         = Executors.newFixedThreadPool(poolSize);
		clients      = new LinkedList<Connection>();
	}

	/** Run the mud. */
	private void run() {
		/* fixme: how to get try-with-resorces to work? */
		try {
			for( ; ; ) {
				Connection client = new Connection(serverSocket.accept(), this);
				clients.add(client);
				pool.execute(client);
			}
		} catch(SocketException e) {
			/* this occurs if the serverSocket is closed; fixme: yes, this is
			 how we shut it down :[ */
			System.err.print("Shutting down.\n");
		} catch(IOException e) {
			System.err.print("Shutting down: " + e + ".\n");
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
				System.err.print("Server socket error. " + e + ".\n");
			}
		}

	}

	/** closes the server; it will detect this, and shutdown */
	public void shutdown() {
		try {
			serverSocket.close();
		} catch(IOException e) {
			System.err.print("414Mud::shutdown: badness. " + e + ".\n");
		}
	}

	/** prints out the mud info */
	public String toString() {
		return "414Mud"; /* fixme: update as more info becomes available */
	}

}
