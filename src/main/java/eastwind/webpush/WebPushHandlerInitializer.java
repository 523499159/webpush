package eastwind.webpush;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 */
public class WebPushHandlerInitializer extends ChannelInitializer<SocketChannel> {

	private Upgrader upgrader;
	private ChannelManager channelManager;
	private WebMessageEncoder webMessageEncoder = new WebMessageEncoder();
	
    public WebPushHandlerInitializer(Upgrader upgrader, ChannelManager channelManager) {
		this.upgrader = upgrader;
		this.channelManager = channelManager;
	}

	@Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(webMessageEncoder);
        pipeline.addLast(new WebPushHandler(upgrader, channelManager));
    }
}
