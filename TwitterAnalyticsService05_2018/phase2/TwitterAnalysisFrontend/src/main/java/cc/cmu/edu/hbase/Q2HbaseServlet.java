package cc.cmu.edu.hbase;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

/*
 * This servelet send queries and respond from Hbase database
 */
public class Q2HbaseServlet extends HttpServlet {

	/*
	 * field for hbase connection
	 */
	private static String zkAddr = System.getenv("HBase_IP");
	private static TableName tweetUidTableName = TableName.valueOf("tweet_db");
	private static TableName tweetTableName = TableName.valueOf("tweet_word_db");
	private static Table tweetUidTable;
	private static Table tweetTable;
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

	public Q2HbaseServlet() throws IOException {
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
		tweetUidTable = Hconn.getTable(tweetUidTableName);
		tweetTable = Hconn.getTable(tweetTableName);
		log("hbase connected");
	}

	/*
	 * @param: request:
	 * localhost:80/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
	 * 
	 * @return: response: TeamCoolCloud,1234-0000-0001
	 * #saas,#cloudcomputing,#cloud,#startup,#bigdata
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		String keywordString = request.getParameter("keywords");
		String number = request.getParameter("n");
		String userId = request.getParameter("user_id");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "text/xml; charset=UTF-8");
		PrintWriter writer = response.getWriter();
		StringBuilder result = new StringBuilder(teamId + "," + teamAWSAccount + "\n");
		/*
		 * malformed request, return team information
		 */
		HashMap<String, Integer> hashtagMap = new HashMap<String, Integer>();

		if (keywordString == null || number == null || keywordString.length() == 0 || number.length() == 0
				|| !number.matches("[0-9]+")) {
			log("Request in malformed");
			writer.write(result.toString());
			writer.close();
			return;
		}

		String[] keywords = keywordString.split(",");
		/*
		 * HBase query with GET
		 */
		for (String keyword : keywords) {
			try {
				Get get1 = new Get(Bytes.toBytes(keyword));
				Result rs1 = tweetTable.get(get1);

				if (!rs1.isEmpty()) {
					Map<byte[], byte[]> map = rs1.getFamilyMap(bColFamily);
					if (map != null) {
						for (Entry<byte[], byte[]> column : map.entrySet()) {
							String hashtag = Bytes.toString(column.getKey()).toLowerCase();
							Integer score = Integer.parseInt(Bytes.toString(column.getValue()));
							if (!hashtagMap.containsKey(hashtag)) {
								hashtagMap.put(hashtag, score);
							} else {
								hashtagMap.put(hashtag, hashtagMap.get(hashtag) + score);
							}
						}
					}
				}
				/*
				 * keyword not found in database, no need to move on
				 */
				else {
					continue;
				}
				
				Get get2 = new Get(Bytes.toBytes(keyword + "," + userId));
				Result rs2 = tweetUidTable.get(get2);
				/*
				 * If word,uid found in database, add value to hashtag map.
				 */
				if (!rs2.isEmpty()) {
					Map<byte[], byte[]> map = rs2.getFamilyMap(bColFamily);
					if (map != null) {
						for (Entry<byte[], byte[]> column : map.entrySet()) {
							String hashtag = Bytes.toString(column.getKey()).toLowerCase();
							Integer score = Integer.parseInt(Bytes.toString(column.getValue()));
							hashtagMap.put(hashtag, hashtagMap.get(hashtag) + score);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				log("HBase IOException!");
			}
		}
		
		/*
		 * Get the top n hashtags based on hashtagmap.value
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
		StringBuilder temp = new StringBuilder();
		for (int i = 0; pq.size() > 0 && i < n; i++) {
			String key = pq.poll().getKey();
			temp.insert(0, "#" + key + ",");
		}
		String res_str = result.append(temp).deleteCharAt(result.length() - 1).toString() + "\n";
		writer.write(res_str);
		
		writer.close();
	}
}
