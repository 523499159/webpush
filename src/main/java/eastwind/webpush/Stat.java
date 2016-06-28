package eastwind.webpush;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class Stat {

	public static AttributeKey<Stat> STAT = AttributeKey.valueOf("Stat");

	public static long getLastRead(Channel channel) {
		Stat stat = channel.attr(STAT).get();
		return stat == null ? -1 : stat.lastRead;
	}

	public static void setLastRead(Channel channel) {
		Stat stat = channel.attr(STAT).get();
		if (stat == null) {
			stat = new Stat();
			channel.attr(STAT).set(stat);
		}
		stat.lastRead = System.currentTimeMillis();
	}

	long lastRead;
}
