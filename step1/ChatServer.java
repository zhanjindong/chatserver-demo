package newinjava7.nio2.chatserver.step1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * One Time Server.<br />
 * 只能维持一个客户端连接，并且客户端关闭服务端也关闭。
 * 
 * @author jdzhan,2014-10-30
 * 
 */
public class ChatServer {

	private Socket socket = null;
	private ServerSocket server = null;
	private DataInputStream streamIn = null;
	private DataOutputStream streamOut = null;

	public ChatServer(int port) {
		try {
			System.out.println("Binding to port " + port + ",please wait...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			System.out.println("Waiting for a client ...");
			socket = server.accept();
			System.out.println("Client accepted:" + socket);
			open();
			streamOut.write("hello".getBytes());
			boolean done = false;
			while (!done) {
				try {
					String line = streamIn.readLine();
					System.out.println(line);
					done = line.equals(".bye");
				} catch (Exception e) {
					e.printStackTrace();
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
		if (streamOut != null) {
			streamOut.close();
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
