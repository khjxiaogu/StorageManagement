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

import java.io.File;

import com.khjxiaogu.sms.Main;
import com.khjxiaogu.sms.pages.SessionManager.Session;
import com.khjxiaogu.webserver.Utils;
import com.khjxiaogu.webserver.web.lowlayer.Request;
import com.khjxiaogu.webserver.web.lowlayer.Response;
import com.khjxiaogu.webserver.wrappers.FilterChain;
import com.khjxiaogu.webserver.wrappers.HttpFilter;

public class PermissionFilter implements HttpFilter {
	String perm;
	public PermissionFilter(String perm) {
		super();
		this.perm = "%"+perm+"%";
	}
	@Override
	public boolean handle(Request arg0, Response arg1, FilterChain arg2) throws Exception {
		// TODO Auto-generated method stub
		Session s=((MainService)arg2.getCurrentContext()).ssms.validateToken(Utils.cookieToMap(arg0.getHeaders().get("cookie")).get("smssession"));
		if(s==null) {
			arg1.write(200,new File(MainService.baseDir,"login.html"));
			return false;
		}
		if(Main.core.hasResult("SELECT Sname FROM STAFF WHERE Authority LIKE ? AND ID = ?",perm,s.uid))
			return true;
		else
			arg1.write(403,"权限不足！");
		return false;
	}
	public static boolean checkPerm(Session s,String perm) {
		if(Main.core.hasResult("SELECT Sname FROM STAFF WHERE Authority LIKE ? AND ID = ?","%"+perm+"%",s.uid))
			return true;
		return false;
	}
}
