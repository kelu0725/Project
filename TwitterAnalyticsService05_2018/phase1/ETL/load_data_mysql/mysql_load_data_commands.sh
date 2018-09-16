# ----- 1------
# load data from the raw data

# 1. download mysql
sudo apt-get update &&
sudo apt-get install mysql-server -y

# 2. load data
wget http://s3.amazonaws.com/cc-team-data/mapreduce-output/tweet_data.tsv &&
mv tweet_data.tsv 3.tsv # download the 3 column records
wget http://s3.amazonaws.com/cc-team-data/mapreduce-output2/tweet_data.tsv
mv tweet_data.tsv 2.tsv # download the merged data
nohup mysql -u root -pmysql --local-infile=1 < mysql_create_table.sql & # load to mysql in backend

# 3. after loading, check the data
mysql> select count(*) from records;
