This is a team project completed by Long Wang, Bingting Wang, Ke Lu in 05/2018.
# Overall Description
* Perform ETL process.
* Build (and optimize) a web service with two components:
  - A front end serving web requests 
  - A back end data storage system serving different queries against the data.
## ETL
Data Source: JSON Twitter dataset of tweets, stored on S3, containing about 200 million tweets
### Steps
* Data Cleaning: remove duplicates, handle malformed data, remove stop words. (Python/Streaming)
* Transform: accomodate to desired schema. (MapReduce)
* Load: import in MySQL(SQL script) and Hbase (Java)
## Web Server
### Query1 Hashtag Recommendation
* Please read [README.md](https://github.com/kelu0725/Project/blob/master/TwitterAnalyticsService05_2018/phase1/README.md)
### Query2 Range Query and Topic Word Extraction
* Please read [README.md](https://github.com/kelu0725/Project/blob/master/TwitterAnalyticsService05_2018/phase2/README.md)
### Query3 Consisten Read/Write Query
* Please read [README.md](https://github.com/kelu0725/Project/blob/master/TwitterAnalyticsService05_2018/phase3/README.md)
## Deployment
* GCP - run Streaming - 13 hrs
* EMR - Hbase Database - 
* AWS - run Backend and Frontend server
## Built With
* [Undertow](http://undertow.io/) - Web Framework used.
* [Maven](https://maven.apache.org/) - Dependency Management.


