# Hashtag Recommendation
## Requirements
* **scoring rules**:
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
* **Target Throughput**: 6000 rps

## Designs
### Architechture
* Database Schema
