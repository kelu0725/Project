#!/usr/bin/env python

# Mapper code to prepare word table.
# Input of this mapper should be output of prepare_word_uid_table_reducer.py
# Each line of data should have 3 columns: word uid  hashtags
# Columns are separated by tab, hashtags are separated by ','


import sys


for line in sys.stdin:
    # split line into word, uid, hashtags
    values = line.strip().split('\t')
    word = values[0]
    uid = values[1]
    hashtags = values[2]
    print '%s\t%s,%s' % (word, uid, hashtags)

        