# Script to set up environment and start the frontend server from a EMR node

# scp frontend code to VM

# use sudo
sudo su 

# install maven
wget http://www.trieuvan.com/apache/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz
tar xzvf apache-maven-3.5.3-bin.tar.gz
sudo mv apache-maven-3.5.3 /usr/local/apache-maven
export PATH=$PATH:/usr/local/apache-maven/bin 

# install maven artifects in repository
mvn clean package

# start server for different framework according to execution id in pom.xml
mvn exec:java@hbase # start hbase server undertow
