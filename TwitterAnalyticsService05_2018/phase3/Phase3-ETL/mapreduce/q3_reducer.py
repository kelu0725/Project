#!/usr/bin/env python
import sys
import json


cur_uid = None
cur_tweets = []


def print_result():
    if (cur_uid != None):
        tweets = "{%s}" % (", ".join(cur_tweets))
        print "%s\t%s" % (cur_uid, tweets)
    
for line in sys.stdin:
    (uid, tweet) = line.strip().split("\t")
    if (uid == cur_uid):
        cur_tweets.append(tweet)
    else:
        print_result()
        cur_uid = uid
        cur_tweets = []
        cur_tweets.append(tweet)
    

print_result()