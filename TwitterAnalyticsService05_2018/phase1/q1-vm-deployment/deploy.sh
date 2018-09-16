# Script to set up environment and start the Query 1 server from a clean Linux VM

# install maven
sudo apt-get update
sudo apt-get install maven

# install jdk and set up environment variable
sudo apt-get install default-jdk
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# install maven artifects in repository
mvn clean package

# start server for different framework according to execution id in pom.xml
sudo mvn exec:java@undertow # start server undertow
sudo mvn exec:java@vertx # start server vertx
