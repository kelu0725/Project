# Hashtag Recommendation
## Requirements
In this query, we are going to predict hashtags for a new tweet by a particular user.
**scoring rules**:
* If a hashtag appears with a keyword, it gets 1 point. 
* If the user_id is the same as the one provided in the input request, the hashtag score for keywords by that user is doubled.
**Target Throughput**: 6000 rps
Sample Request:
```
GET/q2?keywords=cloudcomputing,saas&n=5&user_id=123456789
```
Sample Response:
```
TeamCoolCloud,1234-0000-0001
#saas,#cloudcomputing,#cloud,#startup,#bigdata
```
## Designs

### Architechture
* Database Schema
