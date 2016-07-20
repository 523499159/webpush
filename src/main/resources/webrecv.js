function longPollingRecv(ip, port, params) {
	var wr = {};
	wr.ip = ip;
	wr.port = port;
	wr.ready = false;
	wr.params = params;
	wr.handlers = {};
	wr.i = 0;
	wr.uid = null;
	wr.uuid = null;
	wr.contact = function() {
		var uri = null;
		if (wr.uid != null && wr.uuid != null) {
			uri = ["http://", ip, ":", port, "/?", params].join("");
			on = true;
		} else {
			uri = [wr.baseUri(), "?", params].join("");
		}
		$.ajax({
			type: "get",
			url: uri,
			success: function(data) {
				if (wr.i != 0) {
					wr.i = 0;
				}
				wr.uid = data.uid;
				wr.uuid = data.uuid;
				var regUri = [wr.baseUri(), "/registers?"].join("");
				var first = true;
				for (var k in wr.handlers) {
					if (first) {
						first = false;
						regUri += "type=" + k;
					} else {
						regUri += "&type=" + k;
					}
				}
				$.ajax({
					type: "get",
					url: regUri,
					async: true,
					success: function() {
						wr.ready = true;
						wr.retrieve();
					},
					error: function(r, t, e) {
						console.warn("on:" + r.status + "," + e);
					}
				});
			},
			error: function(r, t, e) {
				wr.ready = false;
				console.warn("contact:" + r.status + "," + e);
				if ((r.status == 403 || r.status == 500) & wr.i > 0) {
					wr.i = 30;
				}
				if (wr.i < 30) {
					wr.i += 2;
				}
				setTimeout(wr.contact, wr.i * 1000);
			}
		});
	}
	wr.retrieve = function() {
		var uri = [wr.baseUri(), "?"].join("");
		$.ajax({
			type: "get",
			url: uri,
			success: function(data) {
				if (!data.sys) {
					var handler = wr.handlers[data.type];
					if (handler != null) {
						handler(data.data);
					}
				}
				wr.retrieve();
			},
			error: function(r, t, e) {
				wr.ready = false;
				wr.uid = null;
				wr.uuid = null;
				console.warn("retrieve:" + r.status + "," + e);
				wr.contact();
			}
		});
	}
	wr.on = function(type, handler) {
		console.info("on:" + type);
		wr.handlers[type] = handler;
		var uri = [wr.baseUri(), "/register?type=", type].join("");
		if (wr.ready) {
			$.ajax({
				type: "get",
				url: uri,
				async: true,
				error: function(r, t, e) {
					console.warn("on:" + r.status + "," + e);
				}
			});
		}
		return wr;
	}
	wr.cancel = function() {
		var url = [wr.baseUri(), "/", "cancel"].join("");
		$.ajax(url);
	}
	wr.baseUri = function() {
		return ["http://", ip, ":", port, "/", wr.uid, "/", wr.uuid].join("");
	}
	$(window).bind("beforeunload", wr.cancel);
	wr.contact();
	return wr;
}

function webSocketRecv(ip, port, params) {
	var wr = {};
	wr.ip = ip;
	wr.port = port;
	wr.params = params;
	wr.socket = null;
	wr.handlers = {};
	wr.i = 0;
	wr.contact = function() {
		var uri = ["ws://", ip, ":", port, "/?", params].join("");
		wr.socket = new WebSocket(uri);
		wr.socket.onmessage = function(event) {
			var message = JSON.parse(event.data);
			if (message.sys) {
				if (message.type == "FORBIDDEN") {
					if (wr.i > 0) {
						wr.i = 30;
					}
					wr.socket.close();
				}
			} else {
				var handler = wr.handlers[message.type];
				if (handler != null) {
					handler(message.data);
				}
			}
		};
		wr.socket.onopen = function() {
			if (wr.i != 0) {
				wr.i = 0;
			}
			var types = [];
			for (var k in wr.handlers) {
				types.push(k);
			}
			var message = {};
			message["type"] = "registers";
			message["data"] = types;
			console.info("on:" + types.join());
			wr.socket.send(JSON.stringify(message));
		}
		wr.socket.onclose = function(event) {
			console.warn("Web Socket closed!");
			if (wr.i < 30) {
				wr.i += 2;
			}
			setTimeout(wr.contact, wr.i * 1000);
		};
	}
	wr.on = function(type, handler) {
		wr.handlers[type] = handler;
		if (wr.socket.readyState == 1) {
			var message = {};
			message["type"] = "register";
			message["data"] = type;
			console.info("on:" + type);
			wr.socket.send(JSON.stringify(message));
		}
		return wr;
	}
	wr.cancel = function() {
		if (wr.socket.readyState == 1) {
			wr.socket.send("{'type' : 'cancel'}");
		}
	}
	$(window).bind("beforeunload", wr.cancel);
	wr.contact();
	return wr;
}

function webRecv(ip, port, params) {
	if (!window.WebSocket) {
		window.WebSocket = window.MozWebSocket;
	}
	if (window.WebSocket) {
		return webSocketRecv(ip, port, params);
	} else {
		return longPollingRecv(ip, port, params);
	}
}