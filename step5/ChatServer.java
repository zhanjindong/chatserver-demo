package newinjava7.nio2.chatserver.step5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * 多路复用。
 * 
 * @author jdzhan,2014-11-4
 * 
 */
public class ChatServer implements Runnable {

	private static final int MAX_CLIENTS = 10;
	private Map<Integer, ChatClient> clients = new HashMap<>();
	private int clientCount = 0;
	private ServerSocketChannel listener;
	private Thread thread;
	private int port;
	private Selector selector;

	private static String serverChannel = "serverChannel";
	private static String channelType = "channelType";

	public ChatServer(int port) throws IOException {
		this.port = port;
		selector = Selector.open();
		this.listener = createListener();
		System.out.println("Binding to port " + port + ", please wait  ...");
		System.out.println("Server started: " + listener);
		start();
	}

	private ServerSocketChannel createListener() throws IOException {
		ServerSocketChannel ls = ServerSocketChannel.open();
		ls.bind(new InetSocketAddress("localhost", port));
		ls.configureBlocking(false);
		SelectionKey socketServerSelectionKey = ls.register(selector, SelectionKey.OP_ACCEPT);
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(channelType, serverChannel);
		socketServerSelectionKey.attach(properties);
		return ls;
	}

	@Override
	public void run() {
		for (;;) {
			if (thread == null) {
				break;
			}
			try {
				if (selector.select() == 0) {
					continue;
				}
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = selectedKeys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					if (((Map) key.attachment()).get(channelType).equals(serverChannel)) {// accept新连接
						ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
						SocketChannel clientSocketChannel = serverSocketChannel.accept();
						if (clientSocketChannel != null) {
							addClient(clientSocketChannel);
						}

					} else {// 读写事件
						SocketChannel clientChannel = (SocketChannel) key.channel();
						if (key.isReadable()) {
							int id = ((InetSocketAddress) clientChannel.getRemoteAddress()).getPort();
							handle(id);
						}
					}
					// once a key is handled, it needs to be removed
					iterator.remove();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void addClient(SocketChannel channel) {
		if (clientCount < MAX_CLIENTS) {
			System.out.println("Client accepted: " + channel);
			try {
				ChatClient client = new ChatClient(this, channel, selector);
				clients.put(client.getID(), client);
				client.open();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else
			System.out.println("Client refused: maximum " + clients.size() + " reached.");
	}

	public synchronized void handle(int id) throws IOException {
		ChatClient client = clients.get(id);
		if (client == null) {
			return;
		}
		String msg = client.read();
		if (msg.equals("q")) {
			clients.get(id).send("bye");
			remove(id);
		} else {
			for (Entry<Integer, ChatClient> c : clients.entrySet()) {
				if (c.getValue().getID() == id) {
					continue;
				}
				c.getValue().send(id + ": " + msg + "\r\n");
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

	@SuppressWarnings("deprecation")
	public void remove(int ID) {
		ChatClient toTerminate = clients.get(ID);
		System.out.println("Removing client thread " + ID);
		try {
			toTerminate.close();
			clients.remove(ID);
		} catch (IOException ioe) {
			System.out.println("Error closing thread: " + ioe);
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

class ChatClient {
	private ChatServer server = null;
	private SocketChannel channel = null;
	private Selector selector;
	private int id = -1;

	private static String clientChannel = "clientChannel";
	private static String channelType = "channelType";

	public ChatClient(ChatServer server, SocketChannel channel, Selector selector) throws IOException {
		super();
		this.server = server;
		this.channel = channel;
		this.selector = selector;
		id = ((InetSocketAddress) channel.getRemoteAddress()).getPort();
	}

	public void send(String msg) throws IOException {
		CharBuffer buffer = CharBuffer.wrap(msg);
		while (buffer.hasRemaining()) {
			channel.write(Charset.defaultCharset().encode(buffer));
		}
		buffer.clear();
	}

	public String read() throws IOException {
		// see if any message has been received
		ByteBuffer bufferA = ByteBuffer.allocate(20);
		int count = 0;
		String message = "";
		while ((count = channel.read(bufferA)) > 0) {
			// flip the buffer to start reading
			bufferA.flip();
			message += Charset.defaultCharset().decode(bufferA);

		}

		return message;
	}

	public int getID() {
		return id;
	}

	public void open() throws IOException {
		channel.configureBlocking(false);
		SelectionKey clientKey = channel.register(selector, SelectionKey.OP_READ, SelectionKey.OP_WRITE);
		Map<String, String> clientproperties = new HashMap<String, String>();
		clientproperties.put(channelType, clientChannel);
		clientKey.attach(clientproperties);
		send("Hello client");
	}

	public void close() throws IOException {
		if (channel != null) {
			channel.close();
		}
	}
}
