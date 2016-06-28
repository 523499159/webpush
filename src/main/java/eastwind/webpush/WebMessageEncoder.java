package eastwind.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Sharable
class WebMessageEncoder extends MessageToMessageEncoder<Message> {

	private static PingWebSocketFrame PING_FRAME = new PingWebSocketFrame();

	private static ByteBuf EMPTY_BUF = Unpooled.copiedBuffer("{}", Charset.forName("utf-8"));
	private static FullHttpResponse EMPTY_RES = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
			EMPTY_BUF);
	static {
		EMPTY_RES.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
		EMPTY_RES.headers().set(HttpHeaderNames.CONTENT_LENGTH, 2);
		EMPTY_RES.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
	}

	private ObjectMapper objectMapper;

	public WebMessageEncoder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
		UserLite userLite = UserLite.get(ctx.channel());
		if (msg.isSys()) {
			Object obj = null;
			if (msg == Message.PING) {
				if (userLite == null) {
					EMPTY_BUF.readerIndex(0);
					obj = EMPTY_RES.retain();
				} else {
					obj = PING_FRAME.retain();
				}
			}
			if (obj != null) {
				out.add(obj);
			}
		} else {
			handleUserMessage(ctx, msg, out, userLite != null);
		}
	}

	private void handleUserMessage(ChannelHandlerContext ctx, Message msg, List<Object> out, Boolean upgraded)
			throws IOException, JsonGenerationException, JsonMappingException {
		ByteBuf buf = ctx.alloc().buffer();
		ByteBufOutputStream os = new ByteBufOutputStream(buf);
		objectMapper.writeValue(os, msg);
		if (upgraded) {
			TextWebSocketFrame tf = new TextWebSocketFrame(buf);
			out.add(tf);
		} else {
			FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
			res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
			res.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
			res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			out.add(res);
		}
	}

}
