package newinjava7.nio2.chatserver.step2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Many Time Server,One Client.<br />
 * 同一时间只能维持一个客户端连接。
 * 
 * @author jdzhan,2014-10-30
 * 
 */
public class ChatServer implements Runnable {

	private Socket socket = null;
	private ServerSocket server = null;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;
	private Thread thread = null;

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

	public void open() throws IOException {
		streamIn = new DataInputStream(socket.getInputStream());
		streamOut = new DataOutputStream(socket.getOutputStream());
	}

	public void close() throws IOException {
		if (socket != null)
			socket.close();
		if (streamIn != null)
			streamIn.close();
		if (streamOut != null) {
			streamOut.close();
		}
	}

	@Override
	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				socket = server.accept();
				System.out.println("Client accepted: " + socket);
				open();
				boolean done = false;
				streamOut.write("hello".getBytes());
				while (!done) {
					try {
						String line = streamIn.readLine();
						System.out.println(line);
						done = line.equals(".bye");
					} catch (IOException ioe) {
						done = true;
					}
				}
				close();
			} catch (IOException ie) {
				System.out.println("Acceptance Error: " + ie);
			}
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
