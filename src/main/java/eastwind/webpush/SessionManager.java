package eastwind.webpush;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

class SessionManager {

	private ConcurrentMap<String, SessionGroup> sessionGroups = Maps.newConcurrentMap();

	public Session create(String uid) {
		SessionGroup us = sessionGroups.get(uid);
		if (us == null) {
			us = new SessionGroup(uid);
			SessionGroup absent = sessionGroups.putIfAbsent(uid, us);
			if (absent != null) {
				us = absent;
			}
			return us.newSession();
		} else {
			Session s = us.newSession();
			if (us.isRemoved()) {
				sessionGroups.remove(us.getUid(), us);
				us = new SessionGroup(uid);
				SessionGroup absent = sessionGroups.putIfAbsent(uid, us);
				if (absent != null) {
					us = absent;
				}
				return us.newSession();
			}
			return s;
		}
	}

	public SessionGroup get(String uid) {
		return sessionGroups.get(uid);
	}

	public void remove(SessionGroup sessionGroup) {
		sessionGroups.remove(sessionGroup.getUid(), sessionGroup);
	}

	public Session get(String uid, String uuid) {
		SessionGroup sg = sessionGroups.get(uid);
		return sg == null ? null : sg.getSession(uuid);
	}

	public void publish(String uid, Message message) {
		SessionGroup sg = sessionGroups.get(uid);
		if (sg != null) {
			sg.publish(message);
		}
	}
}
