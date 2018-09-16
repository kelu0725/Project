# Project

This is a team project completed by Long Wang, Bingting Wang, Ke Lu in 05/2018.

# Overall Description
Build (and optimize) a web service with two components:
- A front end serving web requests 
- A back end data storage system serving different queries against the data.

## ETL
Data Source: JSON Twitter dataset of tweets, stored on S3, containing about 200 million tweets
### Steps
1. Data Cleaning: remove duplicates, handle malformed data, remove stop words. 
2. MapReduce to transform to desired format and stored in S3
3. Load into MySQL and HBase

## 

## Deployment

GCP - run MapReduce
EMR - Hbase Database
AWS - run Backend and Frontend server

## Built With
Undertow - 
[Maven](https://maven.apache.org/) - Dependency Management



```
asdf
````

