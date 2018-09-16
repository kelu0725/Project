package cc.cmu.edu.mysql;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Map.Entry;
/*
 * This servelet send queries and respond from MySQL database
 */
public class Q2MysqlServlet extends HttpServlet {
	private List<Connection> connectionPool = new ArrayList<Connection>();

	/*
	 * field for connection
	 */
	static final String DNS = "localhost";
	static final String dbname = "twitter_db";
	static final String jdbcDriver = "com.mysql.jdbc.Driver";
	static final String jdbcURL = "jdbc:mysql://" + DNS + ":3306/" + dbname + "?useSSL=false&characterEncoding=utf8";
	static final String USER = System.getenv("mysql_user");
	static final String PASSWD = System.getenv("mysql_password");

	/*
	 * field for team information
	 */
	static final String teamId = System.getenv("teamId");
	static final String teamAWSAccount = System.getenv("teamAWSAccount");

	public void log(String log) {
		System.out.println(log);
	}

	public Q2MysqlServlet() throws IOException {}
	
	private synchronized Connection getConnection() {
        if (connectionPool.size() > 0) {
            return connectionPool.remove(connectionPool.size() - 1);
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(jdbcURL, USER, PASSWD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	private synchronized void releaseConnection(Connection con) {
		connectionPool.add(con);
    }
	/*
	 * @param:
	 * request: localhost:80/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
	 * localhost:8080/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
	 * @return:
	 * 
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
		
		String[] keywords = keywordString.split(",");
		StringBuilder keywords_str = new StringBuilder();
		keywords_str.append("(");
		for (int i = 0; i < keywords.length; i++) {
			keywords_str.append("?,");
		}
		keywords_str.setCharAt(keywords_str.length()-1, ')');

		try {
			/*
			 * query mysql for keywords
			 */
			Connection con = getConnection();
			PreparedStatement pstmt = con.prepareStatement("SELECT * FROM q2_tweet WHERE word in " + keywords_str);
			for (int i = 0; i < keywords.length; i++) {
				pstmt.setString(i+1, keywords[i]);
			}
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String[] hashtagAll = rs.getString("hashtags").split(";");
				for (String hashtag : hashtagAll) {
					String[] tokens = hashtag.split(",");
					String uid = tokens[0];
					boolean double_score = uid.equals(userId);
					for (int i = 1; i < tokens.length; i++) {
						String[] tmp_tokens = tokens[i].split(":");
						String tag = tmp_tokens[0].toLowerCase();
						int score = Integer.parseInt(tmp_tokens[1]);
						Integer prev_score = hashtagMap.get(tag);
						if (double_score) score *= 2;
						if (prev_score == null) {
							hashtagMap.put(tag, score);
						} else
							hashtagMap.put(tag, prev_score + score);
					}
				}
			}
			rs.close();
			pstmt.close();
			releaseConnection(con);
		} catch (SQLException e) {
			log("mySQLException");
		}

		/*
		 * Combine hashtags.
		 */
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

		/*
		 * write result
		 */
		StringBuilder temp = new StringBuilder();
		for (int i = 0; pq.size() > 0 && i < n; i++) {
			String key = pq.poll().getKey();
			temp.insert(0, "#" + key + ",");
		}

		result.append(temp).deleteCharAt(result.length() - 1);
		String res_str = result.toString() + "\n";
		writer.write(res_str);
		writer.close();
	}
}
