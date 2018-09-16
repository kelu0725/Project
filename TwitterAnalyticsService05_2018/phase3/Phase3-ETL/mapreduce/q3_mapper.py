#!/usr/bin/env python
import sys
import json



for line in sys.stdin:
    (tid, time, uid, text, IS, TF) = line.strip().split("\t")
    #data_json = json.dumps({"tid": tid, "time": time, "IS": IS})
    #print data_json
    tweet = '"%s": {"time": "%s", "text": %s, "IS": "%s", "TF": "%s"}' % (tid, time, text, IS, TF)
    print "%s\t%s" % (uid, tweet)

