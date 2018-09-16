
package cc.cmu.edu.mysql;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
//import cc.cmu.edu.mysql.CacheLinkedHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/*
 * This servelet send queries and respond from mysql database
 *localhost:80/q3?uid_start=2317544238&uid_end=2319391795&time_start=1402126179&time_end=1484854251&n1=10&n2=8
 *
 *http://dns.of.instance/q3?uid_start=21919458&uid_end=22049006&time_start=1396503830&time_end=1424866940&n1=7&n2=10
 */
public class Q3MysqlServlet extends HttpServlet {

	/*
	 * Hashset to store censor words
	 */
	private static Set<String> censor_words = new HashSet<>();
	
	private List<Connection> connectionPool = new ArrayList<Connection>();
	/*
	 * field for connection
	 */
	static final String DNS = "localhost";
	static final String dbname = "twitter_db";
	static final String jdbcDriver = "com.mysql.jdbc.Driver";
	static final String USER = System.getenv("user");
	static final String PASSWD = System.getenv("password");
	static final String jdbcURL = "jdbc:mysql://" + DNS + ":3306/" + dbname + "?useSSL=false&characterEncoding=utf8";

	// static HikariDataSource datasource;
	static Connection con = null;
	/*
	 * field for team information
	 */
	static final String teamId = System.getenv("teamId");
	static final String teamAWSAccount = System.getenv("teamAWSAccount");

	public void log(String log) {
		System.out.println(log);
	}

	public Q3MysqlServlet() throws Exception {
		/*
		 * Initialization mysql connection
		 */

		if (connectionPool.size() > 0) {
			connectionPool.remove(connectionPool.size() - 1);
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(jdbcURL, USER, PASSWD);
			/*
			 * Read censor words from file.
			 */
			FileInputStream in = null;
			in = new FileInputStream("src/main/resources/censorwords.txt");
			Scanner scanner = new Scanner(in, "utf-8");
			while (scanner.hasNext()) {
				censor_words.add(scanner.next());
			}
			scanner.close();

		} catch (Exception e) {
			log("file error");
		}
	}
	 private synchronized void releaseConnection(Connection con) {
	        connectionPool.add(con);
	    }
	/*
	 * @param: request:
	 * localhost/q3?uid_start=21919458&uid_end=22049006&time_start=1396503830&
	 * time_end=1424866940&n1=7&n2=10
	 *
	 * localhost/q3?uid_start=21919458&uid_end=22049006&time_start=1396503830&
	 * time_end=1424866940&n1=7&n2=10
	 *
	 * http://dns.of.instance/q3?uid_start=492520982&uid_end=492620184&time_start=
	 * 1483124687&time_end=1484856522&n1=hello&n2=5
	 *
	 * http://dns.of.instance/q3?uid_start=whatiseven&uid_end=happening&time_start=
	 * 1483124687&time_end=1484856522&n1=20&n2=word
	 *
	 * @return: response: TeamCoolCloud,1234-0000-0001
	 * #saas,#cloudcomputing,#cloud,#startup,#bigdata
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "text/xml; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		String team_info = teamId + "," + teamAWSAccount + "\n";
		String uid_start_str = request.getParameter("uid_start"), uid_end_str = request.getParameter("uid_end"),
				time_start_str = request.getParameter("time_start"), time_end_str = request.getParameter("time_end"),
				n1_str = request.getParameter("n1"), n2_str = request.getParameter("n2");
		/*
		 * malformed request, return team information
		 */
		if (uid_start_str == null || uid_end_str == null || time_start_str == null || time_end_str == null
				|| n1_str == null || n2_str == null || uid_start_str.compareTo(uid_end_str) > 0
				|| time_start_str.compareTo(time_end_str) > 0 || !n1_str.matches("[0-9]+") || !n2_str.matches("[0-9]+")
				|| !uid_start_str.matches("[0-9]+") || uid_end_str == null || !time_end_str.matches("[0-9]+")) {
			log("Request in malformed");
			writer.write(team_info);
			writer.close();
			return;
		}
		int n1 = Integer.parseInt(n1_str), n2 = Integer.parseInt(n2_str);
		long time_start = Long.parseLong(time_start_str), time_end = Long.parseLong(time_end_str),
				uid_start = Long.parseLong(uid_start_str), uid_end = Long.parseLong(uid_end_str);

		int size = 0;
		/*
		 * Priority queue to store tweets.
		 */
		TweetComparator tweetComparator = new TweetComparator();
		PriorityQueue<Tweet> tweet_pq = new PriorityQueue<>(11, tweetComparator);
		/*
		 * Hashmap to store total number of tweets with word in it.
		 */
		HashMap<String, Float> term_tweet_freq = new HashMap<>();
		/*
		 * query mysql in the range of timestamp and the range of users
		 */
		long time = System.currentTimeMillis();
		long current_time = 0L;
		try {
			PreparedStatement pstmt = con.prepareStatement(
					"SELECT text, freq, score, tid FROM q3_tweet FORCE INDEX (uid_time) WHERE (uid BETWEEN ? AND ?) AND (time BETWEEN ? AND ?)");
			pstmt.setLong(1, uid_start);
			pstmt.setLong(2, uid_end);
			pstmt.setLong(3, time_start);
			pstmt.setLong(4, time_end);
			ResultSet rs = pstmt.executeQuery();
			current_time = System.currentTimeMillis();

			while (rs.next()) {
				size++;
				String text = rs.getString("text");
				String TF_str = rs.getString("freq");
				int impactScore = rs.getInt("score");
				Long tid = rs.getLong("tid");
				Tweet tweet = new Tweet(tid, text, impactScore);
				String[] TFs = TF_str.split(",");
				float total_cnt = Integer.parseInt(TFs[0].split(":")[1]);
				for (String TF : TFs) {
					String[] tokens = TF.split(":");
					if (tokens[0].equals("*"))
						continue;
					tweet.addTermFrequency(tokens[0], Integer.parseInt(tokens[1]) / total_cnt); // add the frequency to
																								// this word
					Float tmp_freq = term_tweet_freq.get(tokens[0]); // number of tweets with the word
					if (tmp_freq != null) {
						term_tweet_freq.put(tokens[0], tmp_freq + 1);// number of tweets with the word
					} else {
						term_tweet_freq.put(tokens[0], (float) 1);
					}
				}
				tweet_pq.add(tweet);
			}
			rs.close();
			pstmt.close();
			releaseConnection(con);

		} catch (Exception e) {
			e.printStackTrace();
			log("mysql IOException!");
		}

		/*
		 * Calculate TF-TDF for each topic word.
		 */
		HashMap<String, Float> topic_score = new HashMap<>();
		double total_tweet_num = size;// number of tweets from rs
		for (Tweet tweet : tweet_pq) {
			for (Entry<String, Float> entry : tweet.term_frequencies.entrySet()) { // the term-frequency
				String word = entry.getKey();
				float TF = entry.getValue();
				float IDF = (float) Math.log(total_tweet_num / term_tweet_freq.get(word));
				float score = (float) (TF * IDF * Math.log(tweet.impactScore + 1.0));
				Float tmp_topic_score = topic_score.get(word);
				if (tmp_topic_score == null) {
					topic_score.put(word, score);
				} else {
					topic_score.put(word, tmp_topic_score + score);
				}
			}
		}
		/*
		 * Select n1 topic words.
		 */
		Comparator<Entry<String, Float>> comparator = new Comparator<Entry<String, Float>>() {
			@Override
			public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {
				if (o1.getValue().equals(o2.getValue())) {
					return o2.getKey().compareTo(o1.getKey());
				}
				return o1.getValue().compareTo(o2.getValue());
			}
		};
		PriorityQueue<Entry<String, Float>> topic_word_pq = new PriorityQueue<>(n1, comparator);
		Set<String> topic_word_set = new HashSet<>();
		for (Entry<String, Float> entry : topic_score.entrySet()) {
			if (topic_word_pq.size() < n1) {
				topic_word_pq.add(entry);
			} else if (comparator.compare(topic_word_pq.peek(), entry) < 0) {
				topic_word_pq.poll();
				topic_word_pq.add(entry);
			}
		}
		StringBuilder topic_word_result = new StringBuilder();
		while (topic_word_pq.size() > 0) {
			Entry<String, Float> entry = topic_word_pq.poll();
			topic_word_result.insert(0,
					censorWord(entry.getKey()) + ":" + String.format("%.2f", entry.getValue()) + "\t");
			topic_word_set.add(entry.getKey());
		}
		topic_word_result.setCharAt(topic_word_result.length() - 1, '\n');
		/*
		 * Get top n2 tweets.
		 */
		StringBuilder tweets_result = new StringBuilder();
		while (tweet_pq.size() > 0) {
			Tweet tweet = tweet_pq.poll();
			boolean has_topic_word = false;
			for (Entry<String, Float> entry : tweet.term_frequencies.entrySet()) {
				if (topic_word_set.contains(entry.getKey())) {
					has_topic_word = true;
					break;
				}
			}
			if (has_topic_word) {
				String tweet_str = StringEscapeUtils.unescapeJava(tweet.text.substring(1, tweet.text.length() - 1));
				tweets_result.append(String.valueOf(tweet.impactScore) + "\t" + tweet.tid + "\t" + tweet_str + "\n");
				n2--;
			}
			if (n2 == 0)
				break;
		}
		tweets_result.deleteCharAt(tweets_result.length() - 1);

		String res_str = team_info + topic_word_result.toString() + tweets_result.toString();
		writer.write(res_str);
		System.out.println("request=" + Long.toString(current_time - time) + "; final="
				+ Long.toString(System.currentTimeMillis() - time) + "; query_num=" + Integer.toString(size));

		writer.close();
	}


	private String censorWord(String word) {
		StringBuilder censored_word = new StringBuilder(word);
		if (censor_words.contains(word)) {
			for (int i = 1; i < word.length() - 1; i++) {
				censored_word.setCharAt(i, '*');
			}
		}
		return censored_word.toString();
	}

	private class Tweet {
		public Long tid;
		public String text;
		public int impactScore;
		public HashMap<String, Float> term_frequencies = new HashMap<>();

		public Tweet(Long t_tid, String t_text, int t_is) {
			tid = t_tid;
			text = t_text;
			impactScore = t_is;
		}

		public void addTermFrequency(String word, float freq) {
			term_frequencies.put(word, freq);
		}
	}

	private class TweetComparator implements Comparator<Tweet> {
		@Override
		public int compare(Tweet t1, Tweet t2) {
			if (t1.impactScore == t2.impactScore) {
				return -t1.tid.compareTo(t2.tid);
			} else {
				return t2.impactScore - t1.impactScore;
			}
		}
	}
}
