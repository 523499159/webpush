package eastwind.webpush;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;

import com.google.common.collect.Maps;

public class ChannelManager {

	private Map<String, ChannelGroup> channelGroups = Maps.newHashMap();
	private Map<String, Channel> channels = Maps.newConcurrentMap();
	
	public void addChannel(final String id, Channel channel) {
		channels.put(id, channel);
		channel.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				channels.remove(id);
			}
		});
	}

	public ChannelGroup getGroup(String group) {
		return getGroup(group, false);
	}
	
	public void joinGroup(String group, Channel channel) {
		ChannelGroup g = getGroup(group, true);
		g.add(channel);
	}
	
	private synchronized ChannelGroup getGroup(String group, boolean create) {
		ChannelGroup g = channelGroups.get(group);
		if (g == null && create) {
			g = new DefaultChannelGroup(group, GlobalEventExecutor.INSTANCE);
			channelGroups.put(group, g);
		}
		return g;
	}
}
