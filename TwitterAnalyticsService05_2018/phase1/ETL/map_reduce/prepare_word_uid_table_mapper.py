# Mapper code to prepare word_uid table.
# Input of this mapper should be filtered data from preprop_tsv.py.
# Each line of data should have 4 columns: uid  hashtags    words   tid
# Columns are separated by tab, words and hashtags are separated by ','


#! /usr/bin/python
import sys


for line in sys.stdin:
    # split line into uid, hashtags, words, tid
    values = line.strip().split('\t')
    uid = values[0]
    hashtags = values[1]
    words = values[2].split(',')
    
    # print record, (word,uid) pair as key
    for word in words:
        if (word != ""):
            print '%s,%s\t%s' % (word, uid, hashtags)
        