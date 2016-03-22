package eastwind.webpush;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WebPush {

	private int port;
	private ServerBootstrap b = new ServerBootstrap();
	private Upgrader upgrader = null;

	private ChannelManager channelManager = new ChannelManager();

	public void start() {
		EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
		b.childHandler(new WebPushHandlerInitializer(upgrader, channelManager));
		b.bind(port).addListener(new GenericFutureListener<ChannelFuture>() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					System.out.println("web push server port:" + port);
				} else {
					future.cause().printStackTrace();
					System.exit(1);
				}
			}
		});
	}

	public void setUpgrader(Upgrader upgrader) {
		this.upgrader = upgrader;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public static void main(String[] args) throws IOException {
		WebPush webPush = new WebPush();
		webPush.setPort(18442);
		Upgrader upgrader = new Upgrader() {
			@Override
			public Upgrade upgrade(QueryStringDecoder decoder) {
				Upgrade upgrade = new Upgrade();
				upgrade.setSuccess(true);
				upgrade.setGroup(decoder.path());
				upgrade.setUid(decoder.parameters().get("uid").get(0));
				return upgrade;
			}
		};
		webPush.setUpgrader(upgrader);
		webPush.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			String line = br.readLine();
			if (line != null && !line.trim().equals("")) {
				ChannelGroup channelGroup = webPush.channelManager.getGroup("/default");
				if (channelGroup != null) {
					channelGroup.writeAndFlush(line);
				}
			}
		}
	}
}
