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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.khjxiaogu.sms.Main;
import com.khjxiaogu.sms.pages.SessionManager.Session;
import com.khjxiaogu.sms.pages.filters.*;
import com.khjxiaogu.webserver.Utils;
import com.khjxiaogu.webserver.annotations.Adapter;
import com.khjxiaogu.webserver.annotations.FilterClass;
import com.khjxiaogu.webserver.annotations.GetBy;
import com.khjxiaogu.webserver.annotations.Header;
import com.khjxiaogu.webserver.annotations.HttpMethod;
import com.khjxiaogu.webserver.annotations.HttpPath;
import com.khjxiaogu.webserver.annotations.PostQuery;
import com.khjxiaogu.webserver.annotations.Query;
import com.khjxiaogu.webserver.wrappers.ResultDTO;
import com.khjxiaogu.webserver.wrappers.inadapters.CurPathIn;

// TODO: Auto-generated Javadoc
/**
 * The Class MainService.
 */
public class MainService extends Service {

	/** The has account. */
	boolean hasAccount;

	/** The ssms. */
	SessionManager ssms = new SessionManager();

	/** The Constant baseDir. */
	public static final File baseDir = new File("pages");

	/**
	 * Instantiates a new main service.
	 * 
	 * @throws SQLException
	 */
	public MainService() throws SQLException {
		super();
		Main.core.executeEx(
							  " CREATE TABLE IF NOT EXISTS STOCK("
							+ "	Mno CHAR(8) PRIMARY KEY,"
							+ "	Mname VARCHAR(30),"
							+ "	Mqty INT, "
							+ "	Sup VARCHAR(40),"
							+ "	SMax INT,"
							+ "	SMin INT,"
							+ "	Cdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
							+ " INDEX S_Time(Cdate))"
				);
		Main.core.executeEx(
							  " CREATE TABLE IF NOT EXISTS STAFF("
							+ "	ID CHAR(40) PRIMARY KEY,"
							+ "	Account VARCHAR(10) NOT NULL UNIQUE,"
							+ "	Pwd VARCHAR(64) NOT NULL,"
							+ "	Sname VARCHAR(30) NOT NULL,"
							+ "	Authority VARCHAR(30) NOT NULL,"
							+ " INDEX A_Account(Account,Pwd))"
							);
		Main.core.executeEx(
							  " CREATE TABLE IF NOT EXISTS OPERATE("
							+ "	Ono INT PRIMARY KEY AUTO_INCREMENT,"
							+ "	Otype VARCHAR(20) NOT NULL,"
							+ "	Mno CHAR(8),"
							+ "	Oqty INT NOT NULL,"
							+ "	ID CHAR(40) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',"
							+ "	Odate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
							+ " FOREIGN KEY(Mno) REFERENCES STOCK(Mno) ON DELETE CASCADE,"
							+ " FOREIGN KEY(ID)REFERENCES STAFF(ID) ON DELETE SET DEFAULT,"
							+ " INDEX OP_Time(Odate))"
				);
		Main.core.executeEx(
							  " CREATE TABLE IF NOT EXISTS BACKUP("
							+ " Bno INT AUTO_INCREMENT PRIMARY KEY,"
							+ " Note TEXT,"
							+ " Bdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
				);
		dropTriggers();
		addTriggers();
		Main.core.executeEx(
						  "CREATE OR REPLACE VIEW OPERATEL AS(SELECT Ono,Otype,Sup,STOCK.Mno AS Mno,Odate,Mname,(-Oqty)AS QTY,STAFF.Sname FROM OPERATE JOIN STAFF ON OPERATE.ID = STAFF.ID JOIN STOCK ON STOCK.Mno = OPERATE.Mno WHERE(Otype = '????????????'OR Otype = '????????????') AND YEAR(Odate) = 2021 AND MONTH(Odate)=6 "
						+ "UNION "
						+ "SELECT Ono,Otype,Sup,STOCK.Mno AS Mno,Odate,Mname,Oqty,STAFF.Sname FROM STOCK,OPERATE,STAFF WHERE STOCK.Mno = OPERATE.Mno AND OPERATE.ID = STAFF.ID AND Otype = '????????????'AND YEAR(Odate) = 2021 AND MONTH(Odate)=6 "
						+ "ORDER BY Odate desc)"
						);
		Main.core.executeEx("DROP PROCEDURE IF EXISTS backupadd");
		Main.core.executeEx(
				  "CREATE PROCEDURE backupadd (IN NNote Text)"
				+ "       BEGIN"
				+ "          INSERT INTO BACKUP(Note) VALUES (NNote);"
				+ "          SELECT last_insert_id() AS id;"
				+ "       END"
				);
		hasAccount = Main.core.hasResult("SELECT * FROM STAFF WHERE Authority LIKE '%A%'");

		// TODO Auto-generated constructor stub
	}

	void dropTriggers() throws SQLException {
		Main.core.executeEx("DROP TRIGGER IF EXISTS STOCKROLLBACK;");
		Main.core.executeEx("DROP TRIGGER IF EXISTS STOCKMINMAX;");
		Main.core.executeEx("DROP TRIGGER IF EXISTS STOCKMINMAXUPDATE;");
		Main.core.executeEx("DROP TRIGGER IF EXISTS UPDATESTOCK;");
	}

	void addTriggers() throws SQLException {
		Main.core.executeEx(
							  "CREATE TRIGGER STOCKROLLBACK BEFORE UPDATE ON STOCK "
							+ "FOR EACH ROW "
							+ "BEGIN "
							+ "IF (NEW.Mqty<0 OR NEW.Mqty>(SELECT SMax FROM STOCK WHERE Mno=NEW.Mno LIMIT 1)) THEN "
							+ "    signal sqlstate '45000';"
							+ "END IF;"
							+ "END"
						);
		Main.core.executeEx(
							  "CREATE TRIGGER STOCKMINMAX BEFORE INSERT ON STOCK "
							+ "FOR EACH ROW "
							+ "BEGIN "
							+ "IF (NEW.SMin>NEW.SMax OR NEW.Mqty<0 OR NEW.Mqty>NEW.SMax) THEN "
							+ "   signal sqlstate '45000';"
							+ "END IF;"
							+ "END"
						);
		Main.core.executeEx(
							  "CREATE TRIGGER STOCKMINMAXUPDATE BEFORE UPDATE ON STOCK "
							+ "FOR EACH ROW "
							+ "BEGIN "
							+ "IF (NEW.SMin>NEW.SMax OR NEW.Mqty<0 OR NEW.Mqty>NEW.SMax) THEN "
							+ "   signal sqlstate '45000';"
							+ "END IF;"
							+ "END"
						);
		Main.core.executeEx(
							  "CREATE TRIGGER UPDATESTOCK BEFORE INSERT "
							+ "ON OPERATE FOR EACH ROW "
							+ "BEGIN "
							+ "UPDATE STOCK SET Mqty = Mqty+NEW.Oqty WHERE STOCK.Mno = NEW.Mno AND NEW.Otype ='????????????';"
							+ "UPDATE STOCK SET Mqty = Mqty-NEW.Oqty WHERE STOCK.Mno = NEW.Mno AND(NEW.Otype ='????????????' OR NEW.Otype = '????????????');"
							+ "END"
						);
	}

	/**
	 * Checks if is null.
	 *
	 * @param s the s
	 * @return true, if is null
	 */
	static boolean isNull(String s) {
		if (s == null || s.length() == 0)
			return true;
		return false;
	}

	/**
	 * Checks for account.
	 *
	 * @param s the s
	 * @return true, if successful
	 */
	public boolean hasAccount(String s) {
		return Main.core.hasResult("SELECT * FROM STAFF WHERE Account = ?", s);
	}

	/**
	 * Checks for stock.
	 *
	 * @param s the s
	 * @return true, if successful
	 */
	public boolean hasStock(String s) {
		return Main.core.hasResult("SELECT * FROM STOCK WHERE Mno = ?", s);
	}

	/**
	 * Checks for backup.
	 *
	 * @param s the s
	 * @return true, if successful
	 */
	public boolean hasBackup(int s) {
		return Main.core.hasResult("SELECT * FROM BACKUP WHERE Bno = ?", s);
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "??????????????????";
	}

	/**
	 * ????????????.
	 */
	/**
	 * @param path the path
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/css")
	@Adapter
	public ResultDTO publicCSS(@GetBy(CurPathIn.class) String path) {
		return new ResultDTO(200, new File(baseDir, "/css" + path));
	}

	/**
	 * Public JS.
	 *
	 * @param path the path
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/js")
	@Adapter
	public ResultDTO publicJS(@GetBy(CurPathIn.class) String path) {
		return new ResultDTO(200, new File(baseDir, "/js" + path));
	}

	/**
	 * Public img.
	 *
	 * @param path the path
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/img")
	@Adapter
	public ResultDTO publicImg(@GetBy(CurPathIn.class) String path) {
		return new ResultDTO(200, new File(baseDir, "/css" + path));
	}

	/**
	 * Public font.
	 *
	 * @param path the path
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/resources/fonts")
	@HttpPath("/Resources/fonts")
	@HttpPath("/fonts")
	@Adapter
	public ResultDTO publicFont(@GetBy(CurPathIn.class) String path) {
		return new ResultDTO(200, new File(baseDir, "/fonts" + path));
	}

	/**
	 * Public img 2.
	 *
	 * @param path the path
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/images")
	@HttpPath("/content/resources/images")
	@HttpPath("/resources/images")
	@HttpPath("/Resources/images")
	@Adapter
	public ResultDTO publicImg2(@GetBy(CurPathIn.class) String path) {
		return new ResultDTO(200, new File(baseDir, "/images" + path));
	}

	/**
	 * ?????????
	 */
	@HttpMethod("GET")
	@HttpPath("/")
	@Adapter
	public ResultDTO rootindex(@GetBy(CurPathIn.class) String path, @Header("cookie") String cookie) {
		if (!hasAccount) {
			return ResultDTO.redirect("/register.html");
		} else {
			if (ssms.validateToken(Utils.cookieToMap(cookie).get("smssession")) != null)
				if (path.equals("/"))
					return ResultDTO.redirect("/index.html");
				else
					return new ResultDTO(200, new File(baseDir, path));
			return new ResultDTO(200, new File(baseDir, "login.html"));
		}
	}

	/**
	 * ?????????????????????.
	 */

	/**
	 * ????????????
	 */
	@HttpMethod("POST")
	@HttpPath("/login")
	@Adapter
	public ResultDTO login(@PostQuery("username") String username, @PostQuery("userpwd") String pw1) {
		try (PreparedStatement login = Main.core.query("SELECT ID FROM STAFF WHERE Account =? AND Pwd = ?", username,
				Utils.SHA256(pw1));) {
			try (ResultSet rs = login.executeQuery()) {
				if (rs.next()) {
					String uid = rs.getString(1);
					Session ses = ssms.createSession(uid);
					ResultDTO rsd = new ResultDTO(200);
					StringBuilder cookie = new StringBuilder("smssession=");
					cookie.append(ses.getAuthToken());
					cookie.append("; HTTPOnly");
					rsd.addHeader("Set-Cookie", cookie.toString());
					return rsd;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(403, "???????????????????????????");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/register")
	@Adapter
	public ResultDTO registerP() {
		return new ResultDTO(200, new File(baseDir, "register.html"));
	}

	/**
	 * ??????
	 */
	@HttpMethod("POST")
	@HttpPath("/register")
	@Adapter
	public ResultDTO register(@PostQuery("username") String username, @PostQuery("name") String name,
			@PostQuery("userpwd") String pw1, @PostQuery("userpwd2") String pw2) {
		if (!hasAccount) {
			if (isNull(username)||isNull(name)||isNull(pw1)||isNull(pw2))
				return new ResultDTO(400, "??????????????????");
			String uid = "00000000-0000-0000-0000-000000000000";
			if (Main.core.execute("INSERT INTO STAFF(ID,Account,Pwd,Sname,Authority)VALUES(?,?,?,?,'ABCDE')", uid,
					username, Utils.SHA256(pw1), name)) {
				Session ses = ssms.createSession(uid);
				ResultDTO rsd = ResultDTO.redirect("/index.html");
				StringBuilder cookie = new StringBuilder("smssession=");
				cookie.append(ses.getAuthToken());
				cookie.append("; HTTPOnly");
				rsd.addHeader("Set-Cookie", cookie.toString());
				hasAccount = true;
				return rsd;
			} else
				return new ResultDTO(400, "??????????????????");

		} else {
			return ResultDTO.redirect("/");
		}
	}

	/**
	 * ??????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/usersn")
	@Adapter
	public ResultDTO UserSname(@GetBy(SessionInAdapter.class) Session s) {
		if (s == null)
			return new ResultDTO(403, "?????????");
		else {
			try (PreparedStatement ps = Main.core.query("SELECT Sname FROM STAFF WHERE ID = ?", s.uid)) {
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						return new ResultDTO(200, rs.getString(1));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new ResultDTO(403, "?????????");
		}
	}

	/**
	 * ??????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/userauth")
	@Adapter
	public ResultDTO UserAuth(@GetBy(SessionInAdapter.class) Session s) {
		if (s == null)
			return new ResultDTO(403, "?????????");
		else {
			try (PreparedStatement ps = Main.core.query("SELECT Authority FROM STAFF WHERE ID = ?", s.uid)) {
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next())
						return new ResultDTO(200, rs.getString(1));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new ResultDTO(403, "?????????");
		}
	}

	/**
	 * ??????
	 */
	@HttpMethod("GET")
	@HttpPath("/logout")
	@Adapter
	public ResultDTO Logout(@GetBy(SessionInAdapter.class) Session s) {
		if (s == null)
			return ResultDTO.redirect("/");
		else {
			ssms.removeSession(s);
			ResultDTO rsd = ResultDTO.redirect("/");
			StringBuilder cookie = new StringBuilder("smssession=");
			cookie.append("; HTTPOnly");
			rsd.addHeader("Set-Cookie", cookie.toString());
			return rsd;
		}
	}

	/**
	 * ????????????.
	 */

	/**
	 * ??????????????????
	 */

	@HttpMethod("GET")
	@HttpPath("/inventorydata")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO InvData() {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT * FROM STOCK order by Mno asc"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("GET")
	@HttpPath("/inventoryquery")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO InvQuery(@Query("in") String name, @Query("si") String supplier) {
		try {
			boolean s1 = true, s2 = true;
			if (name == null || name.length() == 0)
				s1 = false;
			if (supplier == null || supplier.length() == 0)
				s2 = false;
			if (s1 && !s2)
				return new ResultDTO(200,
						Main.core.queryJSON("SELECT * FROM STOCK WHERE Mname = ? order by Cdate desc", name));
			if (s2 && !s1)
				return new ResultDTO(200,
						Main.core.queryJSON("SELECT * FROM STOCK WHERE Sup = ? order by Cdate desc", supplier));
			if (s1 && s2)
				return new ResultDTO(200, Main.core.queryJSON(
						"SELECT * FROM STOCK WHERE Sup = ? AND Mname = ? order by Cdate desc", supplier, name));
			return new ResultDTO(200, new JsonArray());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("POST")
	@HttpPath("/addinventory")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO addInv(@PostQuery("name") String name, @PostQuery("no") String no, @PostQuery("num") String num,
			@PostQuery("min") String min, @PostQuery("max") String max, @PostQuery("supply") String supply,
			@PostQuery("date") String date) {
		if (isNull(name) || isNull(no) || isNull(num) || isNull(min) || isNull(max) || isNull(supply) || isNull(date))
			return new ResultDTO(400, "????????????????????????");
		if (hasStock(no))
			return new ResultDTO(400, "??????ID????????????");
		if (Main.core.execute("INSERT INTO STOCK(Mno,Mname,Mqty,Sup,Smax,Smin,Cdate)VALUES(?,?,?,?,?,?,?)", no, name,
				num, supply, max, min, new Timestamp(Long.parseLong(date))))
			return new ResultDTO(200, "?????????");
		return new ResultDTO(400, "????????????????????????");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/delinventory")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO delInv(@Query("id") String id) {
		if (!hasStock(id))
			return new ResultDTO(400, "??????ID????????????");
		if (Main.core.execute("DELETE FROM STOCK WHERE Mno = ?", id))
			return new ResultDTO(200, "?????????");
		return new ResultDTO(500, "????????????????????????????????????");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("POST")
	@HttpPath("/modinventory")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO modInv(@PostQuery("name") String name, @PostQuery("no") String no, @PostQuery("num") String num,
			@PostQuery("min") String min, @PostQuery("max") String max, @PostQuery("supply") String supply) {
		if (isNull(name) || isNull(no) || isNull(num) || isNull(min) || isNull(max) || isNull(supply))
			return new ResultDTO(400, "????????????????????????");
		if (!hasStock(no))
			return new ResultDTO(400, "??????ID???????????????");
		if (Main.core.execute("UPDATE STOCK SET Mname = ?,Mqty = ?,Sup = ?,SMax = ?,SMin = ? WHERE Mno = ?", name, num,
				supply, max, min, no))
			return new ResultDTO(200, "?????????");
		return new ResultDTO(400, "????????????????????????");
	}

	/**
	 * ????????????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/itemquerymod")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO ItemQueryM(@Query("id") String mno) {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT * FROM STOCK WHERE Mno=?", mno));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ???????????????
	 */

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("GET")
	@HttpPath("/inboundquery")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO InBoundQuery(@GetBy(SessionInAdapter.class) Session s) {
		try {
			if (PermissionFilter.checkPerm(s, "D"))
				return new ResultDTO(200, Main.core.queryJSON(
						"SELECT Ono,Mname,Sup,OPERATE.Mno AS Mno,Oqty,Odate,Otype,Sname FROM STOCK,OPERATE,STAFF WHERE STAFF.ID = OPERATE.ID AND STOCK.Mno = OPERATE.Mno AND Otype = '????????????'"));
			else if (PermissionFilter.checkPerm(s, "C"))
				return new ResultDTO(200, Main.core.queryJSON(
						"SELECT Ono,Mname,Sup,OPERATE.Mno AS Mno,Oqty,Odate,Otype,Sname FROM STOCK,OPERATE,STAFF WHERE STAFF.ID = OPERATE.ID AND STOCK.Mno = OPERATE.Mno AND Otype = '????????????' AND OPERATE.ID = ?",
						s.uid));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 * 
	 */
	@HttpMethod("GET")
	@HttpPath("/outboundquery")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO OutBoundQuery(@GetBy(SessionInAdapter.class) Session s) {
		try {
			if (PermissionFilter.checkPerm(s, "D"))
				return new ResultDTO(200, Main.core.queryJSON(
						"SELECT Ono,Mname,Sup,OPERATE.Mno AS Mno,Oqty,Odate,Otype,Sname FROM STOCK,OPERATE,STAFF WHERE STAFF.ID = OPERATE.ID AND STOCK.Mno = OPERATE.Mno AND (Otype = '????????????' OR Otype = '????????????')"));
			else if (PermissionFilter.checkPerm(s, "B"))
				return new ResultDTO(200, Main.core.queryJSON(
						"SELECT Ono,Mname,Sup,OPERATE.Mno AS Mno,Oqty,Odate,Otype,Sname FROM STOCK,OPERATE,STAFF WHERE STAFF.ID = OPERATE.ID AND STOCK.Mno = OPERATE.Mno AND (Otype = '????????????' OR Otype = '????????????') AND OPERATE.ID = ?",
						s.uid));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("POST")
	@HttpPath("/putin")
	@FilterClass(In.class)
	@Adapter
	public ResultDTO putinbound(@PostQuery("id") String mno, @PostQuery("num") String qty,
			@PostQuery("type") String type, @PostQuery("date") String date, @GetBy(SessionInAdapter.class) Session s) {
		if (isNull(mno) || isNull(qty) || isNull(type) || isNull(date))
			return new ResultDTO(400, "????????????????????????");
		if (!hasStock(mno))
			return new ResultDTO(400, "??????????????????");
		if (Main.core.execute("INSERT INTO OPERATE(Otype,Mno,Oqty,ID,Odate)VALUES(?,?,?,?,?)", type, mno,
				Integer.valueOf(qty), s.uid, new Timestamp(Long.valueOf(date)))) {
			return new ResultDTO(200);
		}
		return new ResultDTO(400, "????????????????????????????????????");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("POST")
	@HttpPath("/putout")
	@FilterClass(Out.class)
	@Adapter
	public ResultDTO putoutbound(@PostQuery("id") String mno, @PostQuery("num") String qty,
			@PostQuery("type") String type, @PostQuery("date") String date, @GetBy(SessionInAdapter.class) Session s) {
		if (isNull(mno) || isNull(qty) || isNull(type) || isNull(date))
			return new ResultDTO(400, "????????????????????????");
		if (!hasStock(mno))
			return new ResultDTO(400, "??????????????????");
		if (Main.core.execute("INSERT INTO OPERATE(Otype,Mno,Oqty,ID,Odate)VALUES(?,?,?,?,?)", type, mno,
				Integer.valueOf(qty), s.uid, new Timestamp(Long.valueOf(date)))) {
			return new ResultDTO(200);
		}
		return new ResultDTO(400, "??????????????????????????????");
	}

	/**
	 * ?????????????????????
	 *
	 * @param s  the s
	 * @param id the id
	 * @return the result DTO
	 */
	@HttpMethod("GET")
	@HttpPath("/rollback")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO RollBackQ(@GetBy(SessionInAdapter.class) Session s, @Query("id") String id) {
		try (PreparedStatement ps = Main.core.query("SELECT Oqty,Mno,ID,Otype FROM OPERATE WHERE Ono=?",
				Integer.parseInt(id)); ResultSet rs = ps.executeQuery()) {
			if (rs.next()) {
				if (!PermissionFilter.checkPerm(s, "D")) {
					if (!rs.getString("id").equals(s.uid))
						return new ResultDTO(403, "???????????????");
				}
				if (rs.getString("Otype").equals("????????????")) {
					if (Main.core.execute("UPDATE STOCK SET Mqty = Mqty-? WHERE STOCK.Mno=?", rs.getInt("Oqty"),
							rs.getString("Mno"))) {
						Main.core.execute("DELETE FROM OPERATE WHERE Ono=?", Integer.parseInt(id));
						return new ResultDTO(200, "???????????????");
					}
				} else {
					if (Main.core.execute("UPDATE STOCK SET Mqty = Mqty+? WHERE STOCK.Mno=?", rs.getInt("Oqty"),
							rs.getString("Mno"))) {
						Main.core.execute("DELETE FROM OPERATE WHERE Ono=?", Integer.parseInt(id));
						return new ResultDTO(200, "???????????????");
					}
				}
			} else
				return new ResultDTO(400, "??????????????????");
		} catch (NumberFormatException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(400, "????????????????????????????????????");
	}

	/**
	 * ???????????????????????????
	 *
	 */
	@HttpMethod("GET")
	@HttpPath("/itemquery")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO ItemQuery(@Query("id") String mno) {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT Mname,Sup FROM STOCK WHERE Mno=?", mno));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 */
	/**
	 * ??????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/itemquerycheck")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO ItemQueryC(@Query("id") String mno) {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT Mname,Sup,Mqty FROM STOCK WHERE Mno=?", mno));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("POST")
	@HttpPath("/checkin")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO checkInput(@PostQuery("id") String mno, @PostQuery("num") String qty,
			@PostQuery("date") String date) {
		if (isNull(mno) || isNull(qty) || isNull(date))
			return new ResultDTO(400, "????????????????????????");
		if (!hasStock(mno))
			return new ResultDTO(400, "??????????????????");
		if (Main.core.execute("UPDATE STOCK SET Mqty = ?, Cdate = ? WHERE Mno = ?", qty,
				new Timestamp(Long.valueOf(date)), mno)) {
			return new ResultDTO(200);
		}
		return new ResultDTO(400, "????????????????????????????????????");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/checkquery")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO CheckQuery() {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT * FROM STOCK WHERE DATEDIFF(CURRENT_DATE,Cdate)>30"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 */
	/**
	 * ????????????.
	 *
	 */
	@HttpMethod("GET")
	@HttpPath("/alertquery")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO AlertQuery() {
		try {
			return new ResultDTO(200,
					Main.core.queryJSON("SELECT Mno,Mname,Sup,Mqty,SMin FROM STOCK WHERE Mqty<=SMin"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/hasalert")
	@FilterClass(Storage.class)
	@Adapter
	public ResultDTO hasAlert() {
		JsonObject jo = new JsonObject();
		jo.addProperty("pd", Main.core.hasResult("SELECT * FROM STOCK WHERE DATEDIFF(CURRENT_DATE,Cdate)>30 LIMIT 1"));
		jo.addProperty("al", Main.core.hasResult("SELECT * FROM STOCK WHERE Mqty<=SMin LIMIT 1"));
		return new ResultDTO(200, jo);
	}

	/**
	 * ??????????????????
	 */
	/**
	 * ??????????????????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/getuser")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO UserQuery(@Query("user") String mno) {
		if (!hasAccount(mno))
			return new ResultDTO(400, "??????????????????");
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT Sname,Authority FROM STAFF WHERE Account=?", mno));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ??????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/userquery")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO userQuery() {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT Account,Sname,Authority FROM STAFF"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("POST")
	@HttpPath("/addclerk")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO addUser(@PostQuery("name") String name, @PostQuery("user") String user,
			@PostQuery("pass") String pass, @PostQuery("auth") String perm) {
		if (isNull(name) || isNull(user) || isNull(pass))
			return new ResultDTO(400, "????????????????????????");
		if (hasAccount(user))
			return new ResultDTO(400, "??????????????????");
		if (Main.core.execute("INSERT INTO STAFF(ID,Account,Pwd,Sname,Authority)VALUES(?,?,?,?,?)",
				UUID.randomUUID().toString(), user, Utils.SHA256(pass), name, perm))
			return new ResultDTO(200, "?????????");
		return new ResultDTO(500, "????????????????????????????????????");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("POST")
	@HttpPath("/deluser")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO delUser(@PostQuery("id") String id) {
		if (isNull(id))
			return new ResultDTO(400, "?????????????????????");
		if (!hasAccount(id))
			return new ResultDTO(400, "??????????????????");
		if (Main.core.execute(
				"UPDATE OPERATE SET ID='00000000-0000-0000-0000-000000000000' WHERE ID=(SELECT ID FROM STAFF WHERE Account = ? LIMIT 1)",
				id))
			if (Main.core.execute("DELETE FROM STAFF WHERE Account = ?", id))
				return new ResultDTO(200, "?????????");
		return new ResultDTO(500, "????????????????????????????????????");
	}

	/**
	 * ????????????
	 *
	 */
	@HttpMethod("POST")
	@HttpPath("/moduser")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO modUser(@PostQuery("name") String name, @PostQuery("user") String user,
			@PostQuery("pass") String pass, @PostQuery("auth") String auth) {
		if (isNull(user))
			return new ResultDTO(400, "?????????????????????");
		if (!hasAccount(user))
			return new ResultDTO(400, "??????????????????");
		if (isNull(pass)) {
			if (Main.core.execute("UPDATE STAFF SET Sname=?, Authority=? WHERE Account = ?", name, auth, user))
				return new ResultDTO(200, "?????????");
		} else if (Main.core.execute("UPDATE STAFF SET Sname=?,Authority=?,Pwd=? WHERE Account = ?", name, auth,
				Utils.SHA256(pass), user))
			return new ResultDTO(200, "?????????");
		return new ResultDTO(400, "???????????????");
	}

	/**
	 * ??????????????????
	 */
	@HttpMethod("POST")
	@HttpPath("/changepass")
	@FilterClass(LoginFilter.class)
	@Adapter
	public ResultDTO changepass(@PostQuery("opass") String op, @PostQuery("npass") String np,
			@GetBy(SessionInAdapter.class) Session s) {
		if (Main.core.hasResult("SELECT * FROM STAFF WHERE ID=? AND Pwd = ?;", s.uid, Utils.SHA256(op))) {
			Main.core.execute("UPDATE STAFF SET Pwd = ? WHERE ID = ?;", Utils.SHA256(np), s.uid);
			return new ResultDTO(200);
		}
		return new ResultDTO(403, "????????????????????????????????????????????????????????????");
	}

	/**
	 * ???????????????
	 */
	/**
	 * ????????????????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/sumquery")
	@FilterClass(Report.class)
	@Adapter
	public ResultDTO SumQuery(@Query("yr") String year, @Query("mo") String month) {
		JsonObject jo = new JsonObject();
		int yr = Integer.parseInt(year);
		int mo = Integer.parseInt(month);
		try {
			jo.add("listall", Main.core.queryJSON(
					"SELECT * FROM OPERATEL WHERE YEAR(Odate) = ? AND MONTH(Odate)=? ORDER BY Odate asc", yr, mo));
			jo.add("listsum", Main.core.queryJSON(
					"SELECT Mno,Mname,Sup,SUM(qty) AS Mqty FROM OPERATEL WHERE YEAR(Odate) = ? AND MONTH(Odate)=? GROUP BY Mno ORDER BY Mno ASC",
					yr, mo));
			return new ResultDTO(200, jo);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	/**
	 * ???????????????
	 */
	@HttpMethod("GET")
	@HttpPath("/backupquery")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO backupQuery() {
		try {
			return new ResultDTO(200, Main.core.queryJSON("SELECT * FROM BACKUP ORDER BY Bdate desc"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	@HttpMethod("GET")
	@HttpPath("/backuprecovery")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO backupRecovery(@Query("id") String idx) {
		int id = Integer.parseInt(idx);
		if (!hasBackup(id))
			return new ResultDTO(400, "??????????????????");
		try (Connection conn = Main.core.createConnection()) {
			dropTriggers();
			try {

				conn.setAutoCommit(false);
				conn.prepareStatement("DELETE FROM OPERATE;").executeLargeUpdate();
				conn.prepareStatement("DELETE FROM STOCK;").executeLargeUpdate();
				conn.prepareStatement("DELETE FROM STAFF;").executeLargeUpdate();
				conn.prepareStatement("INSERT INTO STOCK SELECT * FROM Mbackup" + id + ";").executeLargeUpdate();
				conn.prepareStatement("INSERT INTO STAFF SELECT * FROM Sbackup" + id + ";").executeLargeUpdate();
				conn.prepareStatement("INSERT INTO OPERATE SELECT * FROM Obackup" + id + ";").executeLargeUpdate();
				conn.commit();
				conn.setAutoCommit(true);
				return new ResultDTO(200);
			} catch (SQLException e) {
				conn.rollback();
				conn.setAutoCommit(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new ResultDTO(500, "SQL Error");
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return new ResultDTO(500, "SQL Error");
		} finally {
			try {
				addTriggers();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@HttpMethod("POST")
	@HttpPath("/backupadd")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO backupAdd(@PostQuery("name") String name) {
		if (isNull(name)) {
			return new ResultDTO(400, "?????????????????????");
		}
		try (Connection conn = Main.core.createConnection()) {
			try {
				conn.setAutoCommit(false);
				PreparedStatement ps = conn.prepareStatement("CALL backupadd(?)");
				ps.setString(1, name);
				ps.execute();
				try (ResultSet rs = ps.getResultSet()) {
					if (rs.next()) {
						int id = rs.getInt(1);
						conn.prepareStatement("CREATE TABLE IF NOT EXISTS Mbackup" + id + " SELECT * FROM STOCK;")
								.executeLargeUpdate();
						conn.prepareStatement("CREATE TABLE IF NOT EXISTS Obackup" + id + " SELECT * FROM OPERATE;")
								.executeLargeUpdate();
						conn.prepareStatement("CREATE TABLE IF NOT EXISTS Sbackup" + id + " SELECT * FROM STAFF;")
								.executeLargeUpdate();
						conn.commit();
						conn.setAutoCommit(true);
						return new ResultDTO(200);
					}
				}

			} catch (SQLException e) {
				conn.rollback();
				conn.setAutoCommit(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}

	@HttpMethod("POST")
	@HttpPath("/backupdel")
	@FilterClass(Admin.class)
	@Adapter
	public ResultDTO backupDel(@PostQuery("id") String idx) {
		int id = Integer.parseInt(idx);
		if (!hasBackup(id))
			return new ResultDTO(400, "??????????????????");
		try (Connection conn = Main.core.createConnection()) {
			try {
				conn.setAutoCommit(false);
				conn.prepareStatement("DROP TABLE IF EXISTS Mbackup" + id + ";").executeLargeUpdate();
				conn.prepareStatement("DROP TABLE IF EXISTS Obackup" + id + ";").executeLargeUpdate();
				conn.prepareStatement("DROP TABLE IF EXISTS Sbackup" + id + ";").executeLargeUpdate();
				PreparedStatement ps = conn.prepareStatement("DELETE FROM BACKUP WHERE Bno = ?");
				ps.setInt(1, id);
				ps.execute();
				conn.commit();
				conn.setAutoCommit(true);
				return new ResultDTO(200);

			} catch (SQLException e) {
				conn.rollback();
				conn.setAutoCommit(true);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new ResultDTO(500, "SQL Error");
	}
}
