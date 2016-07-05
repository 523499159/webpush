# webpush
Netty单端口同时提供HTTP和Websocket推送功能

# client
import webrecv.js

	var wr = webRecv("127.0.0.1", 18442, "uid=123");
	wr.on("test", function(data) {
		// do...
	});
	
# server
	WebPush webPush = new WebPush();
	webPush.setAction(new Action() {
		@Override
		public String active(SocketAddress remoteAddress, String params) {
			QueryStringDecoder decoder = new QueryStringDecoder("?" + params);
			// check and return uid
			return decoder.parameters().get("uid").get(0);
		}
	});
	webPush.setPort(18442);
	webPush.start();
	
	// send data by json
	webPush.publish("123", "test", "this is data");

# maven
	<repositories>
		<repository>
			<id>maven-repository</id>
			<url>https://raw.github.com/breeze-hj/webpush-repo/master</url>
		</repository>
	</repositories>
	
	<dependency>
		<groupId>eastwind</groupId>
		<artifactId>webpush</artifactId>
		<version>1.0.0-ALPHA</version>
	</dependency>