# This script includes ommands used to load data into HBase table
# We used two tables in this project: tweet_db and tweet_word_db
# tweet_db uses 'word,uid' as ROW_KEY, and stores each hashtag as qualifier of one column, and its score as the cell value
# tweet_word_db uses 'word' as ROW_KEY, and stores each hashtag as qualifier of one column, and its score as the cell value
# Loading processes are exactly same for two tables, and we take 'tweet_db' as an example here

# cd import_word_uid_table folder
mvn clean package
# scp import_scv.jar to HBase cluster Master node


# ssh to HBase cluster Master node
hadoop distcp s3://cc-team-data/mapreduce-output1/tweet_data.tsv /user/hadoop/tweet_data.tsv

hbase shell
create 'tweet_db', 'data'
exit

hadoop jar import_csv.jar cc.cmu.edu.utils.YetAnotherImportCsv /user/hadoop/tweet_data.tsv <private ip of master node> tweet_db data


# create HBase table snapshot and export to s3
hbase snapshot create -n tweet_db_snapshot -t tweet_db
hbase snapshot info -snapshot tweet_db_snapshot
hbase snapshot export -snapshot tweet_db_snapshot -copy-to s3://cc-team-hbase-backups/ -mappers 10

# restore HBase table snapshot from s3
sudo -u hbase hbase snapshot export -D hbase.rootdir=s3://cc-team-hbase-backups/ -snapshot tweet_db_snapshot -copy-to hdfs://<public DNS of master node>:8020/user/hbase -mappers 10

hbase shell
create 'tweet_db', 'data'
disable 'tweet_db'
restore_snapshot 'tweet_db'
enable 'tweet_db'
exit
