/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package eastwind.webpush;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Handles handshakes and messages
 */
class WebPushHandler extends SimpleChannelInboundHandler<Object> {

	private static Logger logger = LoggerFactory.getLogger(WebPushHandler.class);

	private SessionManager sessionManager;

	private Action action;
	private HashedWheelTimer timer;
	private ObjectMapper objectMapper;
	private WebSocketServerHandshaker handshaker;
	private int tickTime;
	private int lost;

	public WebPushHandler(Action action, HashedWheelTimer timer, SessionManager sessionManager,
			ObjectMapper objectMapper, int tickTime) {
		this.action = action;
		this.timer = timer;
		this.sessionManager = sessionManager;
		this.objectMapper = objectMapper;
		this.tickTime = tickTime;
		this.lost = tickTime * 5 / 2;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("---------- channel actived:{}", ctx.channel());
		ChannelPinger cp = new ChannelPinger(ctx.channel());
		timer.newTimeout(cp, tickTime, TimeUnit.MILLISECONDS);
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("xxxxxxxxxx channel inactived:{}", ctx.channel());
		super.channelInactive(ctx);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws JsonProcessingException {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws JsonProcessingException {
		// Handle a bad request.
		if (!req.decoderResult().isSuccess()) {
			sendHttpResponse(ctx, req,
					new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}

		// Allow only GET methods.
		if (req.method() != HttpMethod.GET) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
			return;
		}

		String uid = null;
		String uuid = null;
		Channel channel = ctx.channel();

		int q = req.uri().indexOf("?");
		String path = req.uri();
		if (q != -1) {
			path = path.substring(0, q);
		}
		List<String> l = Lists.newLinkedList(Splitter.on("/").omitEmptyStrings().trimResults().split(path));
		if (l.size() >= 2) {
			uid = l.get(0);
			uuid = l.get(1);
			Session s = sessionManager.get(uid, uuid);
			if (s == null) {
				logger.info("expired:{}-{}", uid, uuid);
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.FORBIDDEN));
			}
			if (l.size() == 2) {
				s.setChannel(channel);
				s.trySendMessages();
			} else {
				String oper = l.get(2);
				if (oper.equals("register")) {
					QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
					List<String> types = decoder.parameters().get("type");
					if (types != null && types.size() > 0) {
						s.registerType(types.get(0));
						logger.debug("{} register {}", uid, types.get(0));
					}
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
							Unpooled.copiedBuffer("{}", Charset.forName("utf-8"))));
				} else if (oper.equals("registers")) {
					QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
					List<String> types = decoder.parameters().get("type");
					if (types != null) {
						logger.debug("{} registers {}", uid, objectMapper.writeValueAsString(types));
						s.registerTypes(types);
					}
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
							Unpooled.copiedBuffer("{}", Charset.forName("utf-8"))));
				}
			}
		} else {
			String params = "";
			if (q != -1) {
				String uri = req.uri();
				if (q < uri.length() - 1) {
					params = uri.substring(q + 1);
				}
			}
			try {
				uid = action.active(channel.remoteAddress(), params);
			} catch (Throwable th) {
				logger.warn("active:", th);
				sendHttpResponse(ctx, req,
						new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
								Unpooled.copiedBuffer(th.getClass().getName(), Charset.forName("utf-8"))));
				return;
			}
			if (uid != null) {
				uuid = sessionManager.create(uid).getUuid();
				logger.info("active:{}-{}", uid, uuid);
				UserSession us = sessionManager.get(uid);
				timer.newTimeout(new UserSessionCleaner(us), lost, TimeUnit.MILLISECONDS);
			}

			// websocket
			if ("Upgrade".equals(req.headers().get(HttpHeaderNames.CONNECTION))
					&& "websocket".equals(req.headers().get(HttpHeaderNames.UPGRADE))) {
				// Handshake
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(
						req, ""), null, true);
				handshaker = wsFactory.newHandshaker(req);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
					return;
				} else {
					handshaker.handshake(channel, req);
					UserLite.set(channel, new UserLite(uid, uuid));
				}
			} else {
				String content = String.format("{\"uid\":\"%s\", \"uuid\":\"%s\"}", uid, uuid);
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
						Unpooled.copiedBuffer(content, Charset.forName("utf-8"))));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		// Check for closing frame
		Channel channel = ctx.channel();
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(channel, (CloseWebSocketFrame) frame.retain());
			return;
		}

		UserLite u = UserLite.get(channel);
		Session s = sessionManager.get(u.uid, u.uuid);
		if (s == null) {
			UserLite.set(channel, null);
			ctx.writeAndFlush(Message.FORBIDDEN);
			return;
		}
		Stat.setLastRead(channel);

		if (frame instanceof PingWebSocketFrame) {
			channel.write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		if (frame instanceof TextWebSocketFrame) {
			TextWebSocketFrame tf = (TextWebSocketFrame) frame;
			ByteBufInputStream is = new ByteBufInputStream(tf.content());
			try {
				Message message = objectMapper.readValue(is, Message.class);
				Object data = message.getData();
				if (message.getType().equals("registers")) {
					s.registerTypes((Collection<String>) data);
					s.trySendMessages();
					s.setChannel(channel);
					logger.debug("{} registers {}", u.uid, objectMapper.writeValueAsString(data));
				} else if (message.getType().equals("register")) {
					if (data != null) {
						s.registerType((String) data);
						logger.debug("{} register {}", u.uid, data);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}

		// Send the response and close the connection if necessary.
		res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json; charset=UTF-8");
		res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		HttpUtil.setContentLength(res, res.content().readableBytes());
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (cause.getClass().equals(IOException.class)) {
			return;
		}
		cause.printStackTrace();
	}

	private static String getWebSocketLocation(FullHttpRequest req, String group) {
		String location = req.headers().get(HttpHeaderNames.HOST) + group;
		return "ws://" + location;
	}

	private class ChannelPinger implements TimerTask {

		private WeakReference<Channel> channelRef;

		public ChannelPinger(Channel c) {
			this.channelRef = new WeakReference<Channel>(c);
		}

		@Override
		public void run(Timeout timeout) throws Exception {
			Channel c = channelRef.get();
			if (c == null || !c.isActive()) {
				return;
			}
			long lastRead = Stat.getLastRead(c);
			if (lastRead == -1) {
				return;
			}
			long now = System.currentTimeMillis();
			if (now - lastRead > lost) {
				c.close();
				return;
			}
			long diff = now - lastRead;
			if (diff > tickTime) {
				c.writeAndFlush(Message.PING);
				timer.newTimeout(this, tickTime, TimeUnit.MILLISECONDS);
			} else {
				timer.newTimeout(this, tickTime - diff, TimeUnit.MILLISECONDS);
			}
		}
	}

	private class UserSessionCleaner implements TimerTask {

		private UserSession us;

		public UserSessionCleaner(UserSession us) {
			this.us = us;
		}

		@Override
		public void run(Timeout timeout) throws Exception {
			us.clean();
			if (us.size() == 0) {
				logger.info("clean:{}", us.getUid());
				us.setRemoved(true);
				if (us.size() == 0) {
					sessionManager.remove(us);
					return;
				} else {
					us.setRemoved(false);
				}
			}
			timer.newTimeout(this, lost, TimeUnit.MILLISECONDS);
		}

	}

}
