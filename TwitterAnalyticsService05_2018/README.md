This is a team project completed by Long Wang, Bingting Wang, Ke Lu in 05/2018.
# Overall Description
* Perform ETL process.
* Build (and optimize) a web service with two components:
  - A front end serving web requests 
  - A back end data storage system serving different queries against the data.
## ETL
Data Source: JSON Twitter dataset of tweets, stored on S3, containing about 200 million tweets
### Steps
* Data Cleaning: remove duplicates, handle malformed data, remove stop words. (Python/Spark)
* Transform: accomodate to desired schema. (MapReduce)
* Load: import in MySQL(SQL script) and Hbase (Java)
## Web Server
### Query1 Hashtag Recommendation
#### Requirements
**scoring rules**:
* If a hashtag appears with a keyword, it gets 1 point. 
* If the user_id is the same as the one provided in the input request, the hashtag score for keywords by that user is doubled.
Sample Request:
```
GET/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
```
Sample Response:
```
TeamCoolCloud,1234-0000-0001
#saas,#cloudcomputing,#cloud,#startup,#bigdata
```
**Target Throughput**: 6000 rps
#### Designs
* Architechture
* Database Schema

### Query2 Range Query and Topic Word Extraction
#### Requirements
For this query, we will make requests to your server that have a time range, uid range, the maximum number of topic words (n1), and the maximum number of tweets that should be returned (n2).

You will have to:

* Find all the tweets posted by a user within the uid range AND within the given time range. 
* Calculate the topic score, which is a modified version of TF-IDF (described later).
* Sort and return at most n1 topic words and return them along with at most n2 sorted tweets, which must contain at least one of those n1 topic words.

Sample Request:
```
http://dns.of.instance/q3?uid_start=2317544238&uid_end=2319391795&time_start=1402126179&time_end=1484854251&n1=10&n2=8
```
Sample Response:
```
[YOUR_TEAM_NAME],[YOUR_TEAM_AWS_ID]
online:1869.58  love:1758.53    camgirl:1692.63 like:1669.67    just:1656.46    august:1419.20  new:1325.65 amp:1270.26 u:1232.91   good:1175.60
871208  815093696718774273  RT @Taysbra: O*G I CANT BELIEVE THAT CAMILA ACTUALLY SPENT THE HOLIDAYS WITH HER FAMILY O*G WHY WOULD SHE SHADE 5H LIKE THAT.
T…
864504  816381993973751812  @randoom15 The Survivor isn't a Town role. That would be like making it so executing a Jester, or Amne made the Jailor lose his executions.
628780  627830371137097729  Retweet to gain just follow everyone who retweets and follow back who follows you

Follow me &amp; @nikystylees to be next
625788  631201207420329989  RT @ManOnHisPath: There's nothing wrong with taking a lot of photos, but make sure you capture moments with your heart, not just your phone
569576  475957144076812288  SEO Tip of the Week: Prioritising &amp; finding keywords with Google keyword planner: SEO Tip of the Week: Google ... http://t.co/KSMnounEtK
561324  627571041536221184  RT @anxietysmind: i basically have two moods, either lets do something spontaneous and awesome, or lets just lay in bad all day and forget …
523782  630954657880223744  N.F.L. Roundup: Patriots Shake Up Their Quarterback Roster: New England released Matt Flynn, who had been on t... http://t.co/D8WqOMh7vi
488978  820242557179494401  RT @MyPlace4U: Ukraine Govt Officials intervened in U.S. Election to help get Hillary Elected. @JoeConchaTV @mtracey @brithume  https://t.c…
```
#### Designs

* **Target Throughput**: 1500 rps

* ### Query3 Consisten Read/Write Query
READ/WRITE/SET/DELETE

**Target Throughtput**:
Q1 28000.
Q2 8000.
Q3 1500.

## Deployment
* GCP - run MapReduce
* EMR - Hbase Database
* AWS - run Backend and Frontend server

## Built With
* [Undertow](http://undertow.io/) - Web Framework used.
* [Maven](https://maven.apache.org/) - Dependency Management.


