package eastwind.webpush;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

public class SessionManager {

	private ConcurrentMap<String, UserSession> userSessions = Maps.newConcurrentMap();

	public Session create(String uid) {
		UserSession us = userSessions.get(uid);
		if (us == null) {
			us = new UserSession(uid);
			UserSession absent = userSessions.putIfAbsent(uid, us);
			if (absent != null) {
				us = absent;
			}
			return us.newSession();
		} else {
			Session s = us.newSession();
			if (us.isRemoved()) {
				userSessions.remove(us.getUid(), us);
				us = new UserSession(uid);
				UserSession absent = userSessions.putIfAbsent(uid, us);
				if (absent != null) {
					us = absent;
				}
				return us.newSession();
			}
			return s;
		}
	}

	public UserSession get(String uid) {
		return userSessions.get(uid);
	}

	public void remove(UserSession userSession) {
		userSessions.remove(userSession.getUid(), userSession);
	}

	public Session get(String uid, String uuid) {
		UserSession us = userSessions.get(uid);
		return us == null ? null : us.getSession(uuid);
	}

	public void publish(String uid, Message message) {
		UserSession us = userSessions.get(uid);
		if (us != null) {
			us.publish(message);
		}
	}
}
