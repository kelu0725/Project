# Hashtag Recommendation
## Requirements
In this query, we are going to predict hashtags for a new tweet by a particular user.
* Target Throughput: 6000 rps
### scoring rules
* If a hashtag appears with a keyword, it gets 1 point. 
* If the user_id is the same as the one provided in the input request, the hashtag score for keywords by that user is doubled.
### Example
Sample Request:
```
GET/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
```
Sample Response:
```
TeamCoolCloud,1234-0000-0001
#saas,#cloudcomputing,#cloud,#startup,#bigdata
```
## Design

### Web-tier Architechture:
* MySQL: Use Replica. Database Server and Frontend Server are on the same instance, 6 instances in total. 
* Hbase: 1 instance as frontend, 6 instances as databases in EMR clusters.
* Deployment: [terraform](https://www.terraform.io/) to provision AWS resources.

### ETL
* Streaming to clean data, takes 13 hours 
* MapReduce to transform data

### Database
### Schema Designs:
* Option1: 1 table with primary key pair {keyword,uid}, do the score aggregation in the frontend

* Option2: 1 tables with primary key pair{keyword, uid}, 1 table with primary key {keyword} and the pre-caculated score
It is better for HBase because it uses two "get" operations rather than one "scan". "scan" is very slow.


