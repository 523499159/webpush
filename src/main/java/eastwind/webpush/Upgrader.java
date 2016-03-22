package eastwind.webpush;

import io.netty.handler.codec.http.QueryStringDecoder;

public interface Upgrader {

	public Upgrade upgrade(QueryStringDecoder decoder);
	
}
