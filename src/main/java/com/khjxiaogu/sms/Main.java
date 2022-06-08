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
package com.khjxiaogu.sms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.khjxiaogu.sms.pages.MainService;
import com.khjxiaogu.webserver.builder.BasicWebServerBuilder;

public class Main {
	static Properties info;
	public static MYSQLCore core;
	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub
		info=new Properties();
		try {
			info.load(new FileInputStream(new File("mysql.properties")));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		
		try {
			Connection conn=DriverManager.getConnection("jdbc:mysql://" + info.getProperty("host","localhost") + ":" + info.getProperty("port","3306") + "/" , info);
			conn.prepareStatement("CREATE DATABASE IF NOT EXISTS "+info.getProperty("database","sms")).execute();
			conn.close();
			core=new MYSQLCore(info);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("数据库连接失败，请配置mysql.properties文件");
			return;
		}
		try {
			BasicWebServerBuilder.build().createWrapperRoot(new MainService()).complete().compile().serverHttp(2465).info("服务端启动完成").await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
