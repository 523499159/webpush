package eastwind.webpush;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebPush {

	private static Logger logger = LoggerFactory.getLogger(WebPush.class);
	private int port;
	private int tickTime = 2 * 60;
	private ServerBootstrap b = new ServerBootstrap();
	private Action action;
	private SessionManager sessionManager = new SessionManager();
	private volatile  boolean started;
	
	public void start() {
		started = true;
		if (action == null) {
			throw new RuntimeException("the action of webpush should configured.");
		}
		EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
		b.childHandler(new WebPushHandlerInitializer(action, sessionManager, tickTime * 1000));
		b.bind(port).addListener(new GenericFutureListener<ChannelFuture>() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					if (port == 0) {
						InetSocketAddress add = (InetSocketAddress) ((ServerChannel)future.channel()).localAddress();
						port = add.getPort();
					}
					logger.info("web push server started");
					logger.info("port    :{}", port);
					logger.info("tickTime:{}", tickTime);
					logger.info("action  :{}", action.getClass().getName());
				} else {
					logger.warn("web push server start failed", future.cause());
					System.exit(1);
				}
			}
		});
	}

	public boolean isOnline(String uid) {
		checkStart();
		return sessionManager.get(uid) != null;
	}
	
	public void publish(String uid, String type, Object data) {
		checkStart();
		sessionManager.publish(uid, new Message(false, type, data));
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTickTime() {
		return tickTime;
	}

	/**
	 * @param tickTime 心跳间隔
	 *            unit:second
	 */
	public void setTickTime(int tickTime) {
		this.tickTime = tickTime;
	}

	private void checkStart() {
		if (!started) {
			start();
		}
	}
	
	public static void main(String[] args) throws IOException {
		WebPush webPush = new WebPush();
		webPush.setAction(new Action() {
			@Override
			public String active(SocketAddress remoteAddress, String params) {
				QueryStringDecoder decoder = new QueryStringDecoder("?" + params);
				return decoder.parameters().get("uid").get(0);
			}
		});
		webPush.setPort(18442);
		webPush.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			br.readLine();
			webPush.publish("123", "test", "this is data");
		}
	}
}
