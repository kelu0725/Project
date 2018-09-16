# Mapper code to prepare word table.
# Input of this mapper should be output of prepare_word_uid_table_reducer.py
# Each line of data should have 3 columns: word uid  hashtags
# Columns are separated by tab, hashtags are separated by ','


#! /usr/bin/python
import sys


for line in sys.stdin:
    # split line into word, uid, hashtags
    values = line.strip().split('\t')
    word = values[0]
    hashtags = values[2]
    print '%s\t%s' % (word, hashtags)

        