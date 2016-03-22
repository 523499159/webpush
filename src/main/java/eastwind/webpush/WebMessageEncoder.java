package eastwind.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.nio.charset.Charset;
import java.util.List;

@Sharable
public class WebMessageEncoder extends MessageToMessageEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
		if (msg != null && (msg instanceof HttpMessage || msg instanceof WebSocketFrame)) {
			ctx.write(msg);
			return;
		}
		Boolean upgraded = ctx.channel().attr(WebPushHandler.UPGRADED).get();
		if (upgraded == null) {
			ByteBuf buf = Unpooled.copiedBuffer(msg.toString(), Charset.forName("utf-8"));
			FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
			res.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
			res.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes());
			res.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			out.add(res);
		} else {
			TextWebSocketFrame tf = new TextWebSocketFrame(msg.toString());
			out.add(tf);
		}
	}

}
