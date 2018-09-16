# mysql ubuntu instance deployment
sudo apt-get update && sudo apt-get install mysql-server

# download data
wget https://s3.amazonaws.com/cc-team-data/Q2/q2_tweet_filtered.tsv
wget https://s3.amazonaws.com/cc-team-data/Q3/Q3_data_final.tsv
wget https://s3.amazonaws.com/cc-team-data/Q4/q4_tweet_sorted.tsv


# load data into mysql
nohup mysql -u root -pmysql --local-infile=1 < load_all.sql &

# configure mysql

sudo vi /etc/mysql/mysql.conf.d/mysqld.cnf
# change max_conn = 3000
sudo service mysql restart

# change instanceId and instanceDNS of Q4MysqlServelet.java file

# deploy frontend
sudo apt-get install maven && sudo apt-get install default-jdk && export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 && mvn clean package

sudo mvn exec:java@mysql
