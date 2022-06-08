/**
 * Storage Management System
 * Copyright (C) 2022  khjxiaogu
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.khjxiaogu.sms.pages;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.khjxiaogu.webserver.Utils;

public class SessionManager {
	List<Session> sessions = Collections.synchronizedList(new ArrayList<Session>());

	public List<Session> getSessions() { return sessions; }

	public List<String> getSessionIDs() { return sessionIDs; }

	List<String> sessionIDs = Collections.synchronizedList(new ArrayList<String>());

	public static class Session {
		public final String uid;
		public final String sid;
		private final SessionManager sm;
		public long last;
		public boolean timeout = false;
		private String token = null;

		public String getAuthToken() {
			if (token == null) {
				MessageDigest digest;
				try {
					digest = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
				StringBuilder comb = new StringBuilder("usid=");
				comb.append(uid);
				comb.append("ssid=");
				comb.append(sid);
				return token = Utils.bytesToHex(digest.digest(comb.toString().getBytes()));
			}
			return token;
		}

		public boolean checkToken(String tok) { return token != null && token.equals(tok); }
		public void remove() {
			sm.removeSession(sm.locateSession(sid,true));
		}
		public Session(SessionManager sm,String uid, String sid, long last) {
			this.sm = sm;
			this.uid = uid;
			this.sid = sid;
			this.last = last;
		}
	}

	public SessionManager() {}

	public int locateSession(String id, boolean mode) {
		if (mode) {
			for (int i = 0; i < sessions.size(); i++)
				if (sessions.get(i).sid.equals(id))
					return i;
		} else
			for (int i = 0; i < sessions.size(); i++)
				if (sessions.get(i).uid.equals(id))
					return i;
		return -1;
	};

	public int auth(String uid, String sid) {
		int six;
		if (sid == null || sid.length() == 0 || uid == null || uid.length() == 0)
			return -1;
		if (!isExistentSid(sid) || (six = locateSession(uid)) == -1)
			return -1;
		if (!getSession(six).sid.equals(sid))
			return -2;
		if (getSession(six).timeout /* || !ksm.getSession(six).host.equals(addr) */) { removeSession(six); return -1; }
		return six;
	}

	public int locateSession(String id) { return locateSession(id, false); };

	public Session validateToken(String token) {
		if (token != null)
			for (Session ses : sessions)
				if (ses.checkToken(token))
					return ses;
		return null;
	}

	public Session removeSession(int index) {
		if (index == -1)
			return null;
		Session ses;
		sessionIDs.remove((ses = sessions.remove(index)).sid);
		return ses;
	}

	public boolean isExistentSid(String sid) { return sessionIDs.contains(sid); }

	public long intrandom(long min, long max) { return (long) (Math.random() * max + min); }

	public Session createSession(String uid) {
		Long sessionID = intrandom(1000000000, 9999999999L);
		String sid;
		while (sessionIDs.contains(sid = sessionID.toString()))
			sessionID = intrandom(1000000000, 9999999999L);
		sessionIDs.add(sid);
		Session ses = new Session(this,uid, sid, getTime());
		sessions.add(ses);
		return ses;
	}

	public Session getSession(int six) { return sessions.get(six); }
	public Session getSessionByUid(String uid) { return sessions.get(this.locateSession(uid)); }
	public Session getSessionBySid(String sid) { return sessions.get(this.locateSession(sid,true)); }
	public long getTime() {
		Date dat = new Date();
		return dat.getTime();
	}

	public void removeSession(Session cls) {
		sessionIDs.remove(cls.sid);
		sessions.remove(cls);
	}
}
