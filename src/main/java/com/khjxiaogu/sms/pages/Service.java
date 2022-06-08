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

import com.khjxiaogu.webserver.loging.SimpleLogger;
import com.khjxiaogu.webserver.web.ServiceClass;

public 
abstract class Service implements ServiceClass{
	SimpleLogger sl;
	public Service() {
		super();
		sl=new SimpleLogger(this.getName());
		// TODO Auto-generated constructor stub
	}
	
	public SimpleLogger getLogger() {
		// TODO Auto-generated method stub
		return sl;
	}


}
