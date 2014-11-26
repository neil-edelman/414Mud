import java.net.Socket;

/** Connections are the people connected to our mud; later we will build a
 charachter around them and put them in the game. */

class Connection implements Runnable {

	private final Socket socket;
	private final String name = Orcish.get();

	Connection(Socket socket) {
		System.err.print(this + " starting up.\n");
		this.socket = socket;
	}

	public void run() {
		System.err.print("Running!\n");
	}

	public String toString() {
		return "Connection " + name;
	}

}
