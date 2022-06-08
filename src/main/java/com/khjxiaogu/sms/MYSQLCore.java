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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class MYSQLCore {
	private String url;
	/**
	 * The connection properties... user, pass, autoReconnect..
	 */
	private Properties info;
	private static final int MAX_CONNECTIONS = 8;
	private final List<Connection> POOL = Collections.synchronizedList(new ArrayList<Connection>());

	public MYSQLCore(Properties config) {
		/*info.put("autoReconnect", "true");
		info.put("user", user);
		info.put("password", pass);
		info.put("useUnicode", "true");
		info.put("characterEncoding", "utf8");*/
		info=config;
		url = "jdbc:mysql://" + config.getProperty("host","localhost") + ":" + config.getProperty("port","3306") + "/" + config.getProperty("database","sms") ;
		for (int i = 0; i < MAX_CONNECTIONS; i++) {
			POOL.add(null);
		}
	}

	/**
	 * Gets the database connection for executing queries on.
	 *
	 * @return The database connection
	 */
	public Connection getConnection() {
		for (int i = 0; i < MAX_CONNECTIONS; i++) {
			Connection connection =POOL.get(i);
			try {
				// If we have a current connection, fetch it
				if (connection != null && !connection.isClosed()) {
					if (connection.isValid(10))
						return connection;
				}
				connection = DriverManager.getConnection(url, info);
				POOL.set(i, connection);
				return connection;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public Connection createConnection() throws SQLException {
		return DriverManager.getConnection(url, info);
	}
	public boolean hasResult(String sql,Object... args) {
		try(PreparedStatement ps=getConnection().prepareStatement(sql)){
			if(args!=null&&args.length>0)
				for(int i=0;i<args.length;i++) {
					ps.setObject(i+1,args[i]);
				}
			try(ResultSet rs=ps.executeQuery()){
				if(rs.next())
					return true;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public boolean execute(String sql,Object... args) {
		try(PreparedStatement ps=getConnection().prepareStatement(sql)){
			if(args!=null&&args.length>0)
				for(int i=0;i<args.length;i++) {
					ps.setObject(i+1,args[i]);
				}
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public boolean execute(String sql) {
		try(PreparedStatement ps=getConnection().prepareStatement(sql)){
			ps.execute();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public boolean executeEx(String sql) throws SQLException {
		try(PreparedStatement ps=getConnection().prepareStatement(sql)){
		ps.executeUpdate();
		return true;
		}
	}
	public PreparedStatement query(String sql,Object... args) throws SQLException{
		PreparedStatement ps=getConnection().prepareStatement(sql);
		if(args!=null&&args.length>0)
			for(int i=0;i<args.length;i++) {
				ps.setObject(i+1,args[i]);
			}
		return ps;
	}
	static Map<Integer,Function<Object,JsonElement>> types=new HashMap<>();
	static {
		types.put(Types.INTEGER,o->new JsonPrimitive((Number)o));
		types.put(Types.BIGINT,o->new JsonPrimitive((Number)o));
		types.put(Types.BIT,o->new JsonPrimitive((Number)o));
		types.put(Types.DECIMAL,o->new JsonPrimitive((Number)o));
		types.put(Types.DOUBLE,o->new JsonPrimitive((Number)o));
		types.put(Types.FLOAT,o->new JsonPrimitive((Number)o));
		types.put(Types.SMALLINT,o->new JsonPrimitive((Number)o));
		types.put(Types.TINYINT,o->new JsonPrimitive((Number)o));
		types.put(Types.VARCHAR,o->new JsonPrimitive((String)o));
		types.put(Types.CHAR,o->new JsonPrimitive((String)o));
		types.put(Types.LONGVARCHAR,o->new JsonPrimitive((String)o));
		types.put(Types.BLOB,o->new JsonPrimitive((String)o));
		types.put(Types.TIMESTAMP,o->new JsonPrimitive(((Timestamp)o).getTime()));
	}
	public JsonArray queryJSON(String sql,Object... args) throws SQLException{
		try(PreparedStatement ps=getConnection().prepareStatement(sql)){
			if(args!=null&&args.length>0)
				for(int i=0;i<args.length;i++) {
					ps.setObject(i+1,args[i]);
				}
			try(ResultSet rs=ps.executeQuery()){
				JsonArray ja=new JsonArray();
				int cc=rs.getMetaData().getColumnCount();
				String[] cs=new String[cc];
				Function<Object,JsonElement>[] marshall=new Function[cc];
				for(int i=1;i<=cc;i++) {
					cs[i-1]=rs.getMetaData().getColumnLabel(i);
					marshall[i-1]=types.get(rs.getMetaData().getColumnType(i));
				}
				
				while(rs.next()){
					JsonObject jo=new JsonObject();
					for(int i=1;i<=cc;i++) {
						jo.add(cs[i-1],marshall[i-1].apply(rs.getObject(i)));
					}
					ja.add(jo);
				}
				return ja;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new JsonArray();
	}
	public boolean hasColumn(String table, String column) throws SQLException {
		if (!hasTable(table))
			return false;
		String query = "SELECT * FROM " + table + " LIMIT 0,1";
		try {
			PreparedStatement ps = getConnection().prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				rs.getString(column); // Throws an exception if it can't find that column
				return true;
			}
		} catch (SQLException e) {
			return false;
		}
		return false; // Uh, wtf.
	}
	public boolean hasTable(String table) throws SQLException {
		ResultSet rs = getConnection().getMetaData().getTables(null, null, "%", null);
		while (rs.next()) {
			if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
				rs.close();
				return true;
			}
		}
		rs.close();
		return false;
	}
}
