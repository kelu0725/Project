package cc.cmu.edu.utils;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Import data file(s) from HDFS to HBase word table.
 *
 * Please note:
 * This is a simplified tool to demonstrate how to write MapReduce jobs to
 * load data into HBase.
 *
 * Usage:
 * {@code hadoop jar import_csv.jar cc.cmu.edu.utils.YetAnotherImportCsv args}
 *
 * args example:
 * /input a.b.c.d followers data
 *
 */
public class YetAnotherImportCsv {

    /**
     * The mapper to tokenize the data input with 2 columns per line.
     *
     * Input format: word \t hashtag1:score1,(hashtag2:score2,...,hashtagN:scoreN)
     * Output format:
     * K: word V: hashtag1:score1
     * ...
     * K: word V: hashtagN:scoreN
     */
    private static class CsvTokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        /**
         * Logger.
         */
        private static final Logger LOGGER = Logger.getLogger(CsvTokenizerMapper.class);
        /**
         * Output K,V.
         */
        private Text outputKey = new Text(), outputValue = new Text();

        /**
         * Called once at the beginning of the task.
         */
        @Override
        protected void setup(Context context
        ) throws IOException, InterruptedException {
            LOGGER.setLevel(Level.WARN);
        }

        /**
         * Called once for each key/value pair in the input split.
         */
        @Override
        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            String[] columns = value.toString().split("\t");
            if (columns.length != 2) {
                LOGGER.warn(String.format("Malformed record: %s", value));
            } else {
                outputKey.set(columns[0]);
                String[] hashtags = columns[1].split(",");
                for (String hashtag : hashtags) {
                    outputValue.set(hashtag);
                    context.write(outputKey, outputValue);
                }
            }
        }
    }

    /**
     * The reducer to read from intermediate KV pairs and write to HBase table.
     */
    private static class HBaseTableReducer extends
            TableReducer<Text, Text, ImmutableBytesWritable> {

        /**
         * This method is called once for each key.
         *
         * Input: intermediate KV pairs (K: 'word' V: 'hashtag:score')
         * Output: write to the table:
         * ROW_KEY: 'word'
         * ColFamily: conf.get("COLUMN_FAMILY")
         * ColQualifer: 'hashtag'
         * Cell Value: 'score'
         */
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context
        ) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            final byte[] CF = Bytes.toBytes (conf.get("COLUMN_FAMILY"));
            Put put = new Put(Bytes.toBytes(key.toString()));
            for (Text value : values) {
                String[] tokens = value.toString().split(":");
                put.addColumn(CF, Bytes.toBytes(tokens[0]), Bytes.toBytes(tokens[1]));
            }
            context.write(null, put);
        }
    }

    /**
     * Main entry.
     *
     * @param args run args
     *             args[0]: HDFS input path, e.g. /input
     *             the path must be a directory, not a file
     *             args[1]: ZooKeeper address
     *             i.e. private ip of the HBase master node
     *             args[2]: HBase table name, e.g. followers
     *             args[3]: ColFamily, e.g. data
     * @throws Exception when IO error occurs
     */
    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        String zkAddr = args[1];
        conf.set("hbase.master", zkAddr + ":14000");
        conf.set("hbase.zookeeper.quorum", zkAddr);
        conf.set("hbase.zookeeper.property.clientport", "2181");
        conf.set("COLUMN_FAMILY", args[3]);
        Job job = Job.getInstance(conf, "hbase import csv");
        job.setJarByClass(YetAnotherImportCsv.class);
        job.setMapperClass(CsvTokenizerMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        TableMapReduceUtil.initTableReducerJob(
                args[2], // output table name
                HBaseTableReducer.class, // reducer class
                job);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}