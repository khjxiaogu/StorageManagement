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

import com.khjxiaogu.sms.pages.SessionManager.Session;
import com.khjxiaogu.webserver.Utils;
import com.khjxiaogu.webserver.web.ServiceClass;
import com.khjxiaogu.webserver.web.lowlayer.Request;
import com.khjxiaogu.webserver.wrappers.InAdapter;

public class SessionInAdapter implements InAdapter {

	@Override
	public Object handle(Request arg0, ServiceClass arg1) throws Exception {
		// TODO Auto-generated method stub
		Session s=((MainService)arg1).ssms.validateToken(Utils.cookieToMap(arg0.getHeaders().get("cookie")).get("smssession"));
		return s;
	}

}
