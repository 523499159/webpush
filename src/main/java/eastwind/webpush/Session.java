package eastwind.webpush;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

class Session {

	public static final int EXPRIRED = 12000;
	
	private String uid;
	private String uuid;
	private long lastClose = -1;
	private Set<String> types = Sets.newHashSet();
	private List<Message> messages = Lists.newLinkedList();

	private GenericFutureListener<ChannelFuture> closeListener = new GenericFutureListener<ChannelFuture>() {
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			lastClose = System.currentTimeMillis();
		}
	};
	
	private WeakReference<Channel> channelRef;

	public Session(String uid, String uuid) {
		this.uid = uid;
		this.uuid = uuid;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public synchronized void trySendMessages() {
		Channel c = getChannel();
		if (c != null && c.isActive()) {
			if (messages.size() > 0) {
				tryCleanMessage();
			}
			if (messages.size() > 0) {
				send(messages.get(0));
			}
		}
	}

	public synchronized void trySendMessage(final Message message) {
		if (message.isSys() || types.contains(message.getType())) {
			if (messages.size() > 0) {
				tryCleanMessage();
			}
			this.messages.add(message);
			send(message);
		}
	}

	public synchronized void sendNow(Message message) {
		Channel c = getChannel();
		if (c != null && c.isActive()) {
			c.writeAndFlush(message);
			if (UserLite.get(c) == null) {
				this.channelRef = null;
			}
		}
	}
	
	public synchronized void tryCleanMessage() {
		long now = System.currentTimeMillis();
		while (messages.size() > 0) {
			if (now - messages.get(0).getTime() > EXPRIRED) {
				messages.remove(0);
			} else {
				break;
			}
		}
	}

	private void send(final Message message) {
		Channel channel = getChannel();
		if (channel != null && channel.isActive()) {
			channel.writeAndFlush(message).addListener(new GenericFutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(ChannelFuture cf) throws Exception {
					if (cf.isSuccess()) {
						removeMessage(message);
						if (UserLite.get(cf.channel()) != null) {
							trySendMessages();
						}
					}
				}
			});
			if (UserLite.get(channel) == null) {
				this.channelRef = null;
			}
		}
	}

	public synchronized void removeMessage(Message message) {
		this.messages.remove(message);
	}

	public synchronized void registerType(String type) {
		types.add(type);
	}

	public synchronized void registerTypes(Collection<String> types) {
		this.types.clear();
		if (types != null) {
			this.types.addAll(types);
		}
	}

	public long getLastClose() {
		return lastClose;
	}

	public List<Message> getMessage() {
		return messages;
	}

	public Channel getChannel() {
		return channelRef == null ? null : channelRef.get();
	}

	public void setChannel(Channel channel) {
		if (channel == null) {
			Channel c = this.channelRef.get();
			if (c != null) {
				c.closeFuture().removeListener(this.closeListener);
			}
		}
		
		this.channelRef = new WeakReference<Channel>(channel);
		channel.closeFuture().addListener(this.closeListener);
		Stat.setLastRead(channel);
		lastClose = -1;
	}
}
