package eastwind.webpush;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

class UserSession {

	private String uid;
	private boolean removed;
	private List<Session> sessions = Lists.newArrayList();

	public UserSession(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public synchronized Session newSession() {
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		Session s = new Session(uid, uuid);
		sessions.add(s);
		return s;
	}

	public synchronized void clean() {
		Iterator<Session> it = sessions.iterator();
		long now = System.currentTimeMillis();
		while (it.hasNext()) {
			Session s = it.next();
			if (s.getLastClose() != -1 && now - s.getLastClose() > 60000) {
				it.remove();
			} else {
				s.tryCleanMessage();
			}
		}
	}
	
	public synchronized Session getSession(String uuid) {
		for (Session s : sessions) {
			if (s.getUuid().equals(uuid)) {
				return s;
			}
		}
		return null;
	}

	public synchronized void publish(Message message) {
		for (Session s : sessions) {
			s.trySendMessage(message);
		}
	}
	
	public synchronized int size() {
		return sessions.size();
	}

	public synchronized boolean isRemoved() {
		return removed;
	}

	public synchronized void setRemoved(boolean removed) {
		this.removed = removed;
	}
}
