
package cc.cmu.edu.mysql;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;



public class Q4MysqlServlet extends HttpServlet {
	// update instanceId for each instance
	private int instanceId = 0;
	private final int INS_NUM = 7;
	private final int DROP_THRESHOLD = 200;
	private final List<String> instanceDNS = new ArrayList<>();
	/*
	 * Executed sequence number for each uuid.
	 */
	private static final HashMap<String, AtomicInteger> uuidSeq = new HashMap<>();
	
	private List<Connection> connectionPool = new ArrayList<>();
	/*
	 * field for connection
	 */
	static final String dbname = "twitter_db";
	static final String jdbcDriver = "com.mysql.jdbc.Driver";
	static final String USER = System.getenv("mysql_user");
	static final String PASSWD = System.getenv("mysql_password");

	/*
	 * field for team information
	 */
	static final String teamId = System.getenv("teamId");
	static final String teamAWSAccount = System.getenv("teamAWSAccount");

	public Q4MysqlServlet() {
		// add instance ips here, order is important
		instanceDNS.add("34.238.241.75");
		instanceDNS.add("54.224.175.118");
		instanceDNS.add("54.224.73.15");
		instanceDNS.add("54.89.224.101");
		instanceDNS.add("34.227.26.79");
		instanceDNS.add("35.173.186.5");
		instanceDNS.add("18.205.113.41");
		
	}
	public void log(String log) {
		System.out.println(log);
	}


	/**
	 * Get connection to Mysql database from connection pool.
	 * @return connection
	 */
	private synchronized Connection getConnection() {
        if (connectionPool.size() > 0) {
            return connectionPool.remove(connectionPool.size() - 1);
        }
        String jdbcURL = "jdbc:mysql://localhost:3306/" + dbname + "?useSSL=false&characterEncoding=utf8";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(jdbcURL, USER, PASSWD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	/**
	 * Release connection and add connection back to connection pool.
	 * @param con
	 */
	private synchronized void releaseConnection(Connection con) {
		connectionPool.add(con);
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "text/xml; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		String team_info = teamId + "," + teamAWSAccount + "\n";
			
		String uuid = request.getParameter("uuid");
		int hash_code = getHashCode(uuid);
		//log(String.valueOf(hash_code));
		
		// handle this request
		if (hash_code == instanceId) {
			
			int seq = Integer.parseInt(request.getParameter("seq"));
			String op = request.getParameter("op");
			
			// READ
			if (op.equals("read")) {
				
				// timeout for acquiring lock, return immediately
				if (acquireLock(uuid, seq) == false) {
					writer.close();
					return;
				}
				// carry out this request
				try {
					Long uid_start = Long.parseLong(request.getParameter("uid1")),
					 	 uid_end = Long.parseLong(request.getParameter("uid2"));
					int n = Integer.parseInt(request.getParameter("n"));
					TweetComparator tweetComparator = new TweetComparator();
					PriorityQueue<Tweet> tweet_pq = new PriorityQueue<>(n, tweetComparator);
					
					// execute query
					Connection conn = getConnection();
					PreparedStatement pstmt = conn.prepareStatement(
							"select * from q4_tweet where uid >= ? and uid <= ?");
					pstmt.setLong(1, uid_start);
					pstmt.setLong(2, uid_end);
					
					ResultSet rs = pstmt.executeQuery();
					while (rs.next()) {
						Long tid = rs.getLong("tid"), 
							 uid = rs.getLong("uid");
						String text = rs.getString("text"), 
							   user_name = rs.getString("user_name"),
							   timestamp = rs.getString("timestamp");
						int retweet_count = rs.getInt("retweet_count"),
							favorite_count = rs.getInt("favorite_count");
						Tweet tweet = new Tweet(tid, timestamp, uid, text, user_name, favorite_count, retweet_count);
						tweet_pq.add(tweet);
					}
			             					
					pstmt.close();
					releaseConnection(conn);
				
					// write result
					StringBuilder tweets_result = new StringBuilder();
					while (tweet_pq.size() > 0) {
						Tweet tweet = tweet_pq.poll();
						tweets_result.append(String.valueOf(tweet.tid) + "\t" + tweet.timestamp + "\t" 
								+ String.valueOf(tweet.uid) + "\t" 
								+ StringEscapeUtils.unescapeJava(tweet.user_name) + "\t"
								+ StringEscapeUtils.unescapeJava(tweet.text) + "\t" 
								+ String.valueOf(tweet.favorite_count) + "\t" 
								+ String.valueOf(tweet.retweet_count) + "\n");
						n--;
						if (n == 0) break;
					}
					
					writer.write(team_info + tweets_result);
					
				} catch (Exception e) {
					e.printStackTrace();
					writer.write(team_info);
				}
				
				releaseLock(uuid, seq);
				writer.close();
			}
			
			// NON-READ, return immediately, use another thread to update database
			else {
				Thread t = new Thread(new Runnable() {
                    public void run() {
        				// timeout for acquiring lock, return immediately
        				if (acquireLock(uuid, seq) == false) {
        					return;
        				}
                    	// WRITE
            			if (op.equals("write")) {
            				try {
            					JSONObject tweet_json = new JSONObject(request.getParameter("payload"));
            					JSONObject user_json = (JSONObject)(tweet_json.get("user"));
            					Long tid = tweet_json.getLong("id"),
            						 uid = user_json.getLong("id");
            					int retweet_count = tweet_json.getInt("retweet_count"), 
            						favorite_count = tweet_json.getInt("favorite_count");
            					String text = tweet_json.getString("text"),
            						   timestamp = tweet_json.getString("created_at"),
            						   user_name = user_json.getString("screen_name");
            					
            					// escape text and user_name
            					user_name = StringEscapeUtils.escapeJava(user_name);
            					text = StringEscapeUtils.escapeJava(text);
            					
            					// execute query
            					Connection conn = getConnection();
            					PreparedStatement pstmt = conn.prepareStatement(
            							"REPLACE into q4_tweet (tid, timestamp, uid, user_name, text, favorite_count, retweet_count)"
            							 + " values (?, ?, ?, ?, ?, ?, ?)");
            					pstmt.setLong(1, tid);
            					pstmt.setString(2, timestamp);
            					pstmt.setLong(3, uid);
            					pstmt.setString(4, user_name);
            					pstmt.setString(5, text);
            					pstmt.setInt(6, favorite_count);
            					pstmt.setInt(7, retweet_count);
            					
            					pstmt.executeUpdate();
            					pstmt.close();
            					releaseConnection(conn);
            					
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
            				
            			}
            			
            			// SET
            			else if (op.equals("set")) {
            				try {
            					Long tid = Long.parseLong(request.getParameter("tid"));
            					String field = request.getParameter("field"),
            						   payload = request.getParameter("payload");
            					
            					// execute query
            					Connection conn = getConnection();
            					PreparedStatement pstmt = conn.prepareStatement(
            							"update q4_tweet set " + field + " = ? where tid = ?");
            					if (field.equals("retweet_count") || field.equals("favorite_count")) {
            						int count = Integer.parseInt(payload);
            						pstmt.setInt(1, count);
            					} else {
            						// escape text
                					payload = StringEscapeUtils.escapeJava(payload);
            						pstmt.setString(1, payload);
            					}
            					pstmt.setLong(2, tid);
            					
            					pstmt.executeUpdate();
            					pstmt.close();
            					releaseConnection(conn);
            					
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
            			}
            			
            			// DELETE
            			else if (op.equals("delete")) {
               				try {
            					Long tid = Long.parseLong(request.getParameter("tid"));
            					
            					// execute query
            					Connection conn = getConnection();
            					PreparedStatement pstmt = conn.prepareStatement(
            							"delete from q4_tweet where tid = ?");
            					pstmt.setLong(1, tid);
            					
            					pstmt.executeUpdate();
            					pstmt.close();
            					releaseConnection(conn);
            					
            				} catch (Exception e) {
            					e.printStackTrace();
            				}
            			}
        				releaseLock(uuid, seq);
                    }
				});
				t.start();
				writer.write(team_info + "success\n");
				writer.close();
			}	
			
		} 
		
		// redirect to another instance for handling this request
		else {
			//log("redirect request to " + instanceDNS.get(hash_code));
			String url_str = "http://" + instanceDNS.get(hash_code) + "/q4?" + request.getQueryString();
			response.sendRedirect(url_str);
			writer.close();
		}
	}


	private int getHashCode(String uuid) {
		return Math.abs(uuid.hashCode()) % INS_NUM;
	}
	
	private boolean acquireLock(String uuid, int seq) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		boolean result = true;
		@SuppressWarnings("unchecked")
		Future<String> future = executor.submit(new Callable() {

		    public String call() throws Exception {
		    	synchronized(uuidSeq){
					if (!uuidSeq.containsKey(uuid)) {
						uuidSeq.put(uuid, new AtomicInteger(0));
					}
				}
				AtomicInteger prev_seq = uuidSeq.get(uuid);
				synchronized(prev_seq) {
					// if current seq is 200 larger than previous one, increment prev_seq
					if (seq - prev_seq.get() > DROP_THRESHOLD) {
						prev_seq.incrementAndGet();
					}
					while (prev_seq.get() != seq - 1 && seq != 1) {
						try {
							log("waiting for uuid: " + uuid + " seq: " + seq + ", current seq is " + prev_seq.get());
							prev_seq.wait();
						} catch (InterruptedException e) {}
					}
				}
		        return "OK";
		    }
		});
		try {
		    future.get(10, TimeUnit.SECONDS); //timeout is in 10 seconds
		} catch (TimeoutException e) {
		    System.err.println("Timeout");
		    result = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		executor.shutdownNow();
		return result;
				
	}
	
	private void releaseLock(String uuid, int seq) {
		AtomicInteger prev_seq = uuidSeq.get(uuid);
		synchronized(prev_seq){
			prev_seq.set(seq);
			prev_seq.notifyAll();
		}
	}
	
	
	private class Tweet {
		public Long tid, uid;
		public String text, user_name, timestamp;
		public int favorite_count, retweet_count;

		public Tweet(Long t_tid, String t_timestamp, Long t_uid, String t_text, String t_un, int t_fc, int t_rc) {
			tid = t_tid;
			timestamp = t_timestamp;
			uid = t_uid;
			text = t_text;
			user_name =  t_un;
			favorite_count = t_fc;
			retweet_count = t_rc;
		}
	}

	private class TweetComparator implements Comparator<Tweet> {
		@Override
		public int compare(Tweet t1, Tweet t2) {
			int t1_score = t1.favorite_count + t1.retweet_count;
			int t2_score = t2.favorite_count + t2.retweet_count;
			if (t1_score == t2_score) {
				return t1.tid.compareTo(t2.tid);
			} else {
				return t2_score - t1_score;
			}
		}
	}
}
