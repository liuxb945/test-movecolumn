package com.abcd.test.movecolumn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App {
	private static Logger log = Logger.getLogger(App.class);

	public static void main(String[] args) {
		try {
			String jdbcUrlF = "jdbc:mysql://192.168.101.42:3306/phoenix_knowledge?characterEncoding=UTF-8";
			String jdbcUsernameF = "dev";
			String jdbcPasswordF = "dev";
			Connection ConnF = null;
			String jdbcUrlT = "jdbc:mysql://192.168.101.131:3306/phoenix_column?characterEncoding=UTF-8";
			String jdbcUsernameT = "zhangwei";
			String jdbcPasswordT = "zhangwei";
			Connection ConnT = null;
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			ConnF = DriverManager.getConnection(jdbcUrlF, jdbcUsernameF, jdbcPasswordF);
			ConnT = DriverManager.getConnection(jdbcUrlT, jdbcUsernameT, jdbcPasswordT);
			String sqlF = "select count(*) as total from tb_column";
			Statement stmt = ConnF.createStatement();
			ResultSet rs = stmt.executeQuery(sqlF);
			while (rs.next()) {
				log.info("phoenix_knowledge.tb_column 总数:" + rs.getLong(1));
			}
			rs.close();
			stmt.close();
			int indexF = 0;
			String sqlF1 = "select * from tb_column order by id asc";
			Statement stmtF1 = ConnF.createStatement();
			ResultSet rsF1 = stmtF1.executeQuery(sqlF1);
			Statement stmtF2 = ConnF.createStatement();
			ResultSet rsF2 = null;
			Statement stmtT2 = ConnT.createStatement();
			String content = "use phoenix_column;\r\n";
			content += "set character set utf8;\r\n";
			content += "truncate table tb_column_self;\r\n";
			Long uid = null;
			while (rsF1.next()) {
				indexF++;
				log.info("取第" + indexF + "条column.id=" + rsF1.getLong("id"));
				String sqlF2 = "select * from tb_column_tag where column_id=" + rsF1.getLong("id");
				rsF2 = stmtF2.executeQuery(sqlF2);
				String tagIds = "";
				int uOrS;
				while (rsF2.next()) {
					// tagIds+=","+rsF2.getLong("id");
					tagIds += "," + rsF2.getString("tag");
				}
				log.info("column.id=" + rsF1.getLong("id") + ",tag.id="
						+ (tagIds.equals("") ? tagIds : tagIds.substring(1)));
				uid = rsF1.getLong("user_id");
				uOrS = 0;
				if (uid != null && uid.longValue() == 0) {
					uOrS = 1;
				}
				// stmtT1.executeUpdate("delete from tb_column_self where
				// id="+rsF1.getLong("id"));
				String sqlT = String.format(
						"INSERT INTO tb_column_self VALUES (%d,%d,NULL,'%s',%d,0,'%s',%d,%d,%d,%d,'%s','%s','%s','%s');%n",
						rsF1.getLong("id"), rsF1.getLong("parent_id"), rsF1.getString("columnName")==null?"":rsF1.getString("columnName").replace("'", ""),
						rsF1.getLong("user_id"), rsF1.getString("path_name")==null?"":rsF1.getString("path_name").replace("'", ""), rsF1.getLong("subscribe_count"),
						rsF1.getInt("type"), uOrS, rsF1.getShort("del_status"),
						rsF1.getTimestamp("createtime").toString(), rsF1.getTimestamp("update_time").toString(),
						rsF1.getString("column_level_path"), (tagIds.equals("") ? tagIds : tagIds.substring(1).replace("'", "")));
				content += sqlT;

			}
			rsF1 = stmtF1.executeQuery("select max(id) from tb_column");
			Long maxId = null;
			while (rsF1.next()) {
				log.info("tb_column maxId:" + rsF1.getLong(1));
				maxId = rsF1.getLong(1);
			}
			URL url = App.class.getResource("/addCol.sql");
			System.out.println(url.getFile());
			File f = new File(url.getFile());
			if (!f.exists()) {
				log.error("未找到addCol.sql");
			} else {
				try {
					FileReader fr = new FileReader(url.getFile());
					BufferedReader br = new BufferedReader(fr);
					String str = "";
					while ((str = br.readLine()) != null) {
						str=str.replaceAll("%s", (++maxId).toString());
						content += str+"\r\n";
					}
					br.close();
					fr.close();
				} catch (IOException e) {
					e.printStackTrace();
					log.error(e.getMessage(),e);
				}
			}
			stmtT2.close();
			//添加用户自定义栏目
			content+="truncate table tb_column_custom;"+"\r\n";
			content+="insert into tb_column_custom(cid,pcid,columnName,user_id,order_num,path_name,subscribe_count,type,user_or_system,del_status,createtime,update_time,column_level_path) ";
			content+="select id,parent_id,columnName,user_id,order_num,path_name,subscribe_count,type,user_or_system,del_status,createtime,update_time,column_level_path from tb_column_self where user_id=0 and user_or_system=1 and parent_id=0;";
			writeContent(content, "d:/log", "columnmove.sql", false);
			log.info("栏目导出成功");
			if (rsF2 != null) {
				rsF2.close();
				stmtF2.close();
			}

			rsF1.close();
			stmtF1.close();
			ConnF.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 写入内容到文件
	 * 
	 * @param number
	 * @param filename
	 * @return
	 */
	public static boolean writeContent(String c, String dirname, String filename, boolean isAppend) {
		File f = new File(dirname);
		if (!f.exists()) {
			f.mkdirs();
		}
		try {
			FileOutputStream fos = new FileOutputStream(dirname + File.separator + filename, isAppend);
			OutputStreamWriter writer = new OutputStreamWriter(fos);
			writer.write(c);
			writer.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
