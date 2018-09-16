package cc.cmu.edu.hbase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

/*
 * This servelet send queries and respond from Hbase database
 */
public class Q3HbaseServlet extends HttpServlet {

	/*
	 * Hashset to store censor words
	 */
	private static Set<String> censor_words = new HashSet<>();
	/*
	 * field for hbase connection
	 */
	private static String zkAddr = System.getenv("HBase_IP");
	private static TableName q3TweetTableName = TableName.valueOf("q3_tweet_uid");
	private static Table q3TweetTable;
	private static Connection Hconn;
	private static byte[] bColFamily = Bytes.toBytes("data");
	private static final Logger LOGGER = Logger.getRootLogger();
	/*
	 * field for team information
	 */
	static final String teamId = System.getenv("teadId");
	static final String teamAWSAccount = System.getenv("teamAWSAccount");

	public void log(String log) {
		System.out.println(log);
	}

	public Q3HbaseServlet() throws IOException {
		/*
		 * HBase Connection
		 */
		LOGGER.setLevel(Level.OFF);
		if (!zkAddr.matches("\\d+.\\d+.\\d+.\\d+")) {
			System.out.print("Malformed HBase IP address");
			System.exit(-1);
		}
		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.master", zkAddr + ":16000");
		conf.set("hbase.zookeeper.quorum", zkAddr);
		conf.set("hbase.zookeeper.property.clientport", "2181");
		Hconn = ConnectionFactory.createConnection(conf);
		q3TweetTable = Hconn.getTable(q3TweetTableName);
		log("hbase connected");
		
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
		log("censor words loaded");
	}

	/*
	 * @param: request:
	 * localhost/q3?uid_start=21919458&uid_end=22049006&time_start=1396503830&time_end=1424866940&n1=7&n2=10
	 * 
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "text/xml; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		String team_info = teamId + "," + teamAWSAccount + "\n";
		
		String 	uid_start = request.getParameter("uid_start"),
				uid_end = request.getParameter("uid_end"),
				time_start = request.getParameter("time_start"),
				time_end = request.getParameter("time_end"),
				n1_str = request.getParameter("n1"),
				n2_str = request.getParameter("n2");
		
		/*
		 * malformed request, return team information
		 */
		if (uid_start == null || uid_end == null || time_start == null || time_end == null
				|| n1_str == null || n2_str == null
				|| uid_start.compareTo(uid_end) > 0 || time_start.compareTo(time_end) > 0
				|| !n1_str.matches("[0-9]+") || !n2_str.matches("[0-9]+")) {
			log("Request in malformed");
			writer.write(team_info);
			writer.close();
			return;
		}
		
		// Padding zeros to uid and timestamp
		uid_start = paddingString(18, uid_start);
		uid_end = paddingString(18, uid_end);
		time_start = paddingString(10, time_start);
		time_end = paddingString(10, time_end);
		
		int n1 = Integer.parseInt(n1_str), n2 = Integer.parseInt(n2_str);

		/*
		 * Scan q3TweetTable table to get valid tids in range
		 */
		String start_row = uid_start + "," + time_start;
		String end_row = uid_end + "," + time_end;
		Scan scan = new Scan().withStartRow(Bytes.toBytes(start_row)).withStopRow(Bytes.toBytes(end_row+1));

		ResultScanner rs = q3TweetTable.getScanner(scan);
		
		/*
		 * Priority queue to store tweets.
		 */
		TweetComparator tweetComparator = new TweetComparator();;
		PriorityQueue<Tweet> tweet_pq = new PriorityQueue<>(1024, tweetComparator);
		/*
		 * Hashmap to store total number of tweets with word in it.
		 */
		HashMap<String, Float> term_tweet_freq = new HashMap<>();
		float total_tweet_num = 0;
		
		for (Result r = rs.next(); r != null; r = rs.next()) {
			String[] tokens = Bytes.toString(r.getRow()).split(",");
			String uid = tokens[0], timestamp = tokens[1];
			if (uid.compareTo(uid_start) >= 0 && uid.compareTo(uid_end) <= 0 &&
					timestamp.compareTo(time_start) >= 0 && timestamp.compareTo(time_end) <= 0) {
				total_tweet_num += 1.0;
				String tid = Bytes.toString(r.getValue(bColFamily, Bytes.toBytes("tid")));
				String text = Bytes.toString(r.getValue(bColFamily, Bytes.toBytes("text")));
				String impactScore = Bytes.toString(r.getValue(bColFamily, Bytes.toBytes("is")));
				String TF_str = Bytes.toString(r.getValue(bColFamily, Bytes.toBytes("TF")));
				Tweet tweet = new Tweet(tid, text, impactScore);
				String[] TFs = TF_str.split(",");
				float total_cnt = Integer.parseInt(TFs[0].split(":")[1]);
				for (String TF : TFs) {
					String[] values = TF.split(":");
					if (values[0].equals("*")) continue;
					tweet.addTermFrequency(values[0], Integer.parseInt(values[1]) / total_cnt);
					Float tmp_freq = term_tweet_freq.get(values[0]);
					if (tmp_freq != null) {
						term_tweet_freq.put(values[0], tmp_freq + 1);
					} else {
						term_tweet_freq.put(values[0], (float) 1);
					}
				}
				tweet_pq.add(tweet);
			}
		}
		rs.close();
		
		/*
		 * Calculate TF-TDF for each topic word.
		 */
		HashMap<String, Float> topic_score = new HashMap<>();
		for (Tweet tweet : tweet_pq) {
			for (Entry<String, Float> entry : tweet.term_frequencies.entrySet()) {
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
			topic_word_result.insert(0, censorWord(entry.getKey()) + ":" + String.format("%.2f", entry.getValue()) + "\t");
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
				String tweet_str = StringEscapeUtils.unescapeJava(tweet.text.substring(1, tweet.text.length()-1));
				tweets_result.append(String.valueOf(tweet.impactScore) + "\t" + tweet.tid + "\t" 
						+ tweet_str + "\n");
				n2--;
			}
			if (n2 == 0) break;
		}
		tweets_result.deleteCharAt(tweets_result.length()-1);
		String res_str = team_info + topic_word_result.toString() + tweets_result.toString();
		writer.write(res_str);

		writer.close();
	}
	
	private String paddingString(int len, String str) {
		String zeros = "000000000000000000";
		return zeros.substring(0, len-str.length()) + str;
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
		public String tid, text;
		public int impactScore;
		public HashMap<String, Float> term_frequencies = new HashMap<>();
		public Tweet(String t_tid, String t_text, String t_is) {
			tid = t_tid;
			text = t_text;
			impactScore = Integer.parseInt(t_is);
		}
		public void addTermFrequency(String word, float freq) {
			term_frequencies.put(word, freq);
		}
	}
	
	private class TweetComparator implements Comparator<Tweet> {

		@Override
		public int compare(Tweet t1, Tweet t2) {
			if (t1.impactScore == t2.impactScore) {
				if (t1.tid.length() != t2.tid.length()) {
					return t1.tid.length() - t2.tid.length();
				} else {
					return -t1.tid.compareTo(t2.tid);
				}
			} else {
				return t2.impactScore - t1.impactScore;
			}
		}
	}
}


