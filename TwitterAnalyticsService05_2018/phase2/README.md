# Range Query and Topic Word Extraction
## Requirements
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
## Designs

* **Target Throughput**: 1500 rps
