package cc.cmu.edu.q2.mysql;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.Comparator;
import java.util.HashMap;

import java.util.PriorityQueue;
import java.util.Map.Entry;
/*
 * This servelet send queries and respond from MySQL database
 */
public class mysqlServlet extends HttpServlet {
	/*
	 * field for connection
	 */
	static final String DNS = "localhost";
	static final String dbname = "twitter_db";
	static final String jdbcDriver = "com.mysql.jdbc.Driver";
	static final String jdbcURL = "jdbc:mysql://" + DNS + ":3306/" + dbname + "?useSSL=false&characterEncoding=utf8";
	static final String USER = System.getenv("USER");
	static final String PASSWD = System.getenv("PASSWORD");
	static HikariDataSource datasource;
	static Connection con = null;
	/*
	 * field for team information
	 */
	static final String teamId = System.getenv("TEAM_ID");
	static final String teamAWSAccount = System.getenv("TEAM_ACCOUNT");
	/*
	 * Cache to store requests and results
	 */
	HashMap<String, String> cache = new HashMap<>();
	
	public void log(String log) {
		System.out.println(log);
	}

	public mysqlServlet() throws IOException {
		/*
		 * Initialization mysql connection
		 */
		try {
			HikariConfig config = new HikariConfig();
			config.setDriverClassName(jdbcDriver);
			config.setJdbcUrl(jdbcURL);
			config.setUsername(USER);
			config.setPassword(PASSWD);
			// config.setMinimumIdle();
			config.setIdleTimeout(0);
			config.setMaxLifetime(0);
			config.setMaximumPoolSize(500);
			config.addDataSourceProperty("cachePrepStmts", "true");
			config.addDataSourceProperty("prepStmtCacheSize", "250");
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
			datasource = new HikariDataSource(config);
			con = datasource.getConnection();
		} catch (Exception e) {
			log("Connection error");
		}
	}
	/*
	 * @param: 
	 * request: localhost:80/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
	 * @return: 
	 * response: TeamCoolCloud,1234-0000-0001
	 * #saas,#cloudcomputing,#cloud,#startup,#bigdata
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		
		String keywordString = request.getParameter("keywords");
		String number = request.getParameter("n");
		String userId = request.getParameter("user_id");

		HashMap<String, Integer> hashtagMap = new HashMap<String, Integer>();
		StringBuilder result = new StringBuilder(teamId + "," + teamAWSAccount + "\n");
		response.setCharacterEncoding("UTF-8");
		PrintWriter writer = response.getWriter();
		/*
		 * malformed request, return team information
		 */
		if (keywordString == null || number == null || "".equals(request.getParameter("keywords")) || "".equals(number)
				|| !number.matches("[0-9]+")) {
			log("Request in malformed");
			writer.write(result.toString());
			writer.close();
		}

		String query = keywordString + "&" + number + "&" + userId;
		String cached_result = cache.get(query);
		/*
		 * If cache contains the request, return cache result.
		 * Else send query to MySQL database, add to cache and return result.
		 */
		if (cached_result != null) {
			writer.write(cached_result);
		}else {
			String[] keywords = keywordString.split(",");
			/*
			 * mySQL schema: word\t uid\t hashtag1:count,hashtag2:count
			 */
			for (String keyword : keywords) {
				try {
					PreparedStatement pstmt = con.prepareStatement("SELECT * FROM nouid WHERE word=?");
					PreparedStatement pstmtUid = con.prepareStatement("SELECT * FROM records WHERE word=? AND uid=?");
					pstmt.setString(1, keyword);
					pstmtUid.setString(1, keyword);
					pstmtUid.setString(2, userId);
					ResultSet rs = pstmt.executeQuery();
					ResultSet uidrs = pstmtUid.executeQuery();
					while (rs.next()) {
						String hashtagString = rs.getString("hashtags");
						String[] hashtagList = hashtagString.split(",");
						for (String x : hashtagList) {
							String hashtag = x.split(":")[0].toLowerCase();
							String count = x.split(":")[1];
							Integer score = Integer.parseInt(count);
							if (!hashtagMap.containsKey(hashtag)) {
								hashtagMap.put(hashtag, score);
							} else
								hashtagMap.put(hashtag, hashtagMap.get(hashtag) + score);
						}
					}
					while (uidrs.next()) {
						String hashtagString = uidrs.getString("hashtags");
						String[] hashtagList = hashtagString.split(",");
						for (String x : hashtagList) {
							String hashtag = x.split(":")[0].toLowerCase();
							String count = x.split(":")[1];
							Integer score = Integer.parseInt(count);
							hashtagMap.put(hashtag, hashtagMap.get(hashtag) + score);
						}
					}
					rs.close();
					uidrs.close();
					pstmt.close();
					pstmtUid.close();
				} catch (SQLException e) {
					log("mySQLException: query keyword=" + keyword);
				}
			}
			int n = Integer.parseInt(number);
			Comparator<Entry<String, Integer>> comparator = new Comparator<Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					if (o1.getValue().equals(o2.getValue())) {
						return o2.getKey().compareTo(o1.getKey());
					}
					return o1.getValue().compareTo(o2.getValue());
				}
			};
			/*
			 * PriorityQueue is used to store n hashtags with largest score.
			 * If the incoming hashtag has a score smaller than the head, discard;
			 * else add the new hashtag and poll the head of PriorityQueue.
			 * If there's a tie, sorted in ascending lexicographical order.
			 */
			PriorityQueue<Entry<String, Integer>> pq = new PriorityQueue<>(n, comparator);
			for (Entry<String, Integer> hashtag : hashtagMap.entrySet()) {
				if (pq.size() < n) {
					pq.add(hashtag);
				} else if (comparator.compare(pq.peek(), hashtag) < 0) {
					pq.poll();
					pq.add(hashtag);
				}
			}
			
			StringBuilder temp = new StringBuilder();
			for (int i = 0; pq.size() > 0 && i < n; i++) {
				String key = pq.poll().getKey();
				temp.insert(0, "#" + key + ",");
			}
			
			result.append(temp).deleteCharAt(result.length() - 1);
			String res_str = result.toString() + "\n";
			writer.write(res_str);
			cache.put(query, res_str);
		}
		writer.close();
	}
}
