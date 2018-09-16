#!/usr/bin/env python

# Reducer code to prepare word table.
# Input of this reducer should be output of prepare_word_table_mapper.py
# Each line of data should have 2 columns: word  hashtags
# Columns are separated by tab, hashtags are separated by ','
# and each tag and its correponding score are separated by ':'

import sys

cur_word = None
cur_tags = []

# print current record
def print_result():
    tag_str = ";".join(cur_tags)
    print "%s\t%s" % (cur_word, tag_str)


for line in sys.stdin:
    values = line.strip().split('\t')
    word = values[0]
    hashtags = values[1]
    
    # same word, reduce hashtags
    if (word == cur_word):
        cur_tags.append(hashtags)
                
    # different word, print result and reinitialize records
    else:
        if (cur_word != None):
            print_result()
        cur_word = word
        cur_tags = []
        cur_tags.append(hashtags)
                
print_result()