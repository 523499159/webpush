package eastwind.webpush;

public class Message {

	public static Message FORBIDDEN = new Message(true, "FORBIDDEN", null);
	public static Message PING = new Message(true, "PING", null);

	private boolean sys;
	private String type;
	private Object data;
	private long time = System.currentTimeMillis();
	
	public Message() {
		super();
	}

	public Message(boolean sys, String type, Object data) {
		this.sys = sys;
		this.type = type;
		this.data = data;
	}

	public boolean isSys() {
		return sys;
	}

	public void setSys(boolean sys) {
		this.sys = sys;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public long getTime() {
		return time;
	}
	
}
