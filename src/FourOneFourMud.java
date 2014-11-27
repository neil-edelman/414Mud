import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** This is the entry-point for starting the mud and listening for connections.
 @author Neil */

class FourOneFourMud {

	static final int fibonacci20    = 6765;
	static final int maxConnections = 256;
	static final int sShutdownTime  = 20;

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
	}

	/** Run the mud. */
	public void run() {
		/* fixme: try-with-resorces */
		try {
			for( ; ; ) {
				pool.execute(new Connection(serverSocket.accept()));
			}
		} catch (IOException e) {
			System.err.print("Shutting down all connections: " + e + ".\n");
			pool.shutdown();
		}
	}

	/** Shutdown the mud; eg, an administrator. */
	public void shutdown() {

		/* fixme: notify the players, too! */
		System.err.print("Shuting down incoming connections.\n");

		/* reject incoming tasks */
		pool.shutdown();

		/* from the ExecutorService JavaSE7 docs */
		try {
			// Wait a while for existing tasks to terminate
			System.err.print("Waiting for threads to terminate.\n");
			if(!pool.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
				System.err.print("Waiting for threads to respond to being terminated.\n");
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if(!pool.awaitTermination(sShutdownTime, TimeUnit.SECONDS)) {
					System.err.print("A thread did not terminate.\n");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/** prints out the mud info */
	public String toString() {
		return "414Mud"; /* fixme: update as more info becomes available */
	}

}
