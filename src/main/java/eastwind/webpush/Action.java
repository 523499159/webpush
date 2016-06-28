package eastwind.webpush;

import java.net.SocketAddress;

public interface Action {

	public String active(SocketAddress remoteAddress, String params);

}
