package newinjava7.nio2.chatserver.step3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * Multi-Server handling Multi-Client.<br />
 * 多线程，一个线程维护一个客户端链接。
 * 
 * @author jdzhan,2014-10-30
 * 
 */
public class ChatServer implements Runnable {

	private ServerSocket server = null;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;
	private Thread thread = null;
	private ChatServerThread client = null;

	public ChatServer(int port) {
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println(ioe);
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

	@Override
	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} catch (IOException ie) {
				System.out.println("Acceptance Error: " + ie);
			}
		}
	}

	public void addThread(Socket socket) {
		System.out.println("Client accepted: " + socket);
		client = new ChatServerThread(this, socket);
		try {
			client.open();
			client.start();
		} catch (IOException ioe) {
			System.out.println("Error opening thread: " + ioe);
		}
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
	private Socket socket = null;
	private ChatServer server = null;
	private int id = -1;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;

	public ChatServerThread(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		id = socket.getPort();
	}

	@Override
	public void run() {
		System.out.println("Server Thread " + id + " running.");
		try {
			boolean done = false;
			streamOut.write(("hello " + id).getBytes());
			while (!done) {
				try {
					@SuppressWarnings("deprecation")
					String line = streamIn.readLine();
					System.out.println(line);
					done = line.equals(".bye");
				} catch (IOException ioe) {
					done = true;
				}
			}
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void open() throws IOException {
		streamIn = new DataInputStream(socket.getInputStream());
		streamOut = new DataOutputStream(socket.getOutputStream());
	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (streamIn != null)
			streamIn.close();
	}
}
