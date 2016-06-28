package eastwind.webpush;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

class UserLite {

	public static AttributeKey<UserLite> USER_LITE = AttributeKey.valueOf("USER_LITE");
	
	public static UserLite get(Channel channel) {
		return channel.attr(USER_LITE).get();
	}
	
	public static void set(Channel channel, UserLite userLite) {
		channel.attr(USER_LITE).set(userLite);;
	}
	
	String uid;
	String uuid;
	
	public UserLite(String uid, String uuid) {
		this.uid = uid;
		this.uuid = uuid;
	}

	public String getUid() {
		return uid;
	}

	public String getUuid() {
		return uuid;
	}
}
