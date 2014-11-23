package newinjava7.nio2.chatserver.step4;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * 多个Socket之间通信。
 * 
 * @author jdzhan,2014-10-30
 * 
 */
public class ChatServer implements Runnable {
	private ChatServerThread clients[] = new ChatServerThread[20];
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;

	public ChatServer(int port) {
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
		}
	}

	@Override
	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} catch (IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;
	}

	public synchronized void handle(int ID, String input) {
		if (input.equals(".bye")) {
			clients[findClient(ID)].send(".bye");
			remove(ID);
		} else
			for (int i = 0; i < clientCount; i++)
				clients[i].send(ID + ": " + input + "\r\n");
	}

	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ChatServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount - 1)
				for (int i = pos + 1; i < clientCount; i++)
					clients[i - 1] = clients[i];
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}
			toTerminate.stop();
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ChatServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else
			System.out.println("Client refused: maximum " + clients.length + " reached.");
	}

	private static void usage() {
		System.err.println("ChatServer [-port <port number>]");
		System.exit(1);
	}

	public static void main(String[] args) throws IOException {
		int port = 5000;
		if (args.length != 0 && args.length != 2) {
			usage();
		} else if (args.length == 2) {
			try {
				if (args[0].equals("-port")) {
					port = Integer.parseInt(args[1]);
				} else {
					usage();
				}
			} catch (NumberFormatException e) {
				usage();
			}
		}
		System.out.println("Running on port " + port);
		new ChatServer(port);
	}
}

class ChatServerThread extends Thread {
	private ChatServer server = null;
	private Socket socket = null;
	private int id = -1;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;

	public ChatServerThread(ChatServer server, Socket socket) {
		super();
		this.server = server;
		this.socket = socket;
		id = socket.getPort();
	}

	public void send(String msg) {
		try {
			streamOut.write(msg.getBytes());
			streamOut.flush();
		} catch (IOException ioe) {
			System.out.println(id + " ERROR sending: " + ioe.getMessage());
			server.remove(id);
			stop();
		}
	}

	public int getID() {
		return id;
	}

	public void run() {
		System.out.println("Server Thread " + id + " running.");
		send("hello " + id);
		while (true) {
			try {
				server.handle(id, streamIn.readLine());
			} catch (IOException ioe) {
				System.out.println(id + " ERROR reading: " + ioe.getMessage());
				server.remove(id);
				stop();
			}
		}
	}

	public void open() throws IOException {
		streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (streamIn != null)
			streamIn.close();
		if (streamOut != null)
			streamOut.close();
	}
}
