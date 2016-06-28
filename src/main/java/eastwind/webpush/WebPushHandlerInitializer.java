package eastwind.webpush;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.HashedWheelTimer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 */
class WebPushHandlerInitializer extends ChannelInitializer<SocketChannel> {

	private Action action;
	private SessionManager sessionManager;
	private WebMessageEncoder webMessageEncoder;
	private HashedWheelTimer timer = new HashedWheelTimer();
	private int tickTime;
	private ObjectMapper objectMapper = new ObjectMapper();

	public WebPushHandlerInitializer(Action action, SessionManager sessionManager, int tickTime) {
		this.action = action;
		this.sessionManager = sessionManager;

		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.enable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
		objectMapper.enable(Feature.ALLOW_SINGLE_QUOTES);
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		webMessageEncoder = new WebMessageEncoder(objectMapper);
		this.tickTime = tickTime;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpObjectAggregator(65536));
		pipeline.addLast(webMessageEncoder);
		pipeline.addLast(new WebPushHandler(action, timer, sessionManager, objectMapper, tickTime));
	}
}
