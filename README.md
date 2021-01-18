# TwitterAnalyticsService05_2018

## Introduction 
-----------
This is a re-organization of team project from course 15619 Cloud Computing @ Carnegie Mellon University. <br>
The project is to build a web service for Twitter data analysis. <br>
About ~1 TB raw twitter data is provided. The dataset will have to be stored within our web service. The web service should be able to handle a specific number of requests per second for several hours. However, the budget is limited, thus the task is to build an effective and cost efficient solution utilizing Amazon Web Services resources. <br>

## Language & Framework
-----------
- Pipeline/MapReduce: Python
- Database Server & Web Server: Java
- Framework: Undertow

## Development Requirement
-----------
The web service solution provides data analysis on the twitter dataset. Users can query resutls based on **userids or time**bold.
- Web Service (API server) should be able to query through HTTP GET request and get response for following:
  * 0. Test web service connection phase, encoding and decoding QR codes.
  * 1. Predict hashtags for a new tweet by a particular user based on all tweets information.
  * 2. Calculate the topic score of requested tweet(specific time range) based on TF-IDF algorithnms. Return highest topic words and tweets.
  * 3. Implement fast interactive service based on MySQL, included read, wirite, set and delete.

- Database Server should be able to handle large concurrent read requests

- ETL(Data extract, transform, load): should be able to handle large data set processing
