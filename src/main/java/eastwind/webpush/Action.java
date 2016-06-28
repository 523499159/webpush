package eastwind.webpush;

import java.net.SocketAddress;

public interface Action {

	/**
	 * 激活,返回值为uid,null视为激活失败
	 * @param remoteAddress
	 * @param params
	 * @return
	 */
	public String active(SocketAddress remoteAddress, String params);

}
