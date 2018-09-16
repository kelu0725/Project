# Reducer code to prepare word_uid table.
# Input of this reducer should be output of prepare_word_uid_table_mapper.py
# Each line of data should have 2 columns: (word,uid)  hashtags
# Columns are separated by tab, hashtags are separated by ','


#! /usr/bin/python
import sys

cur_label = None
cur_tags = dict()

# print current record
def print_result():
    tag_str = ""
    for k,v in cur_tags.items():
        tag_str +=  "%s:%d," % (k, v)
    tag_str = tag_str[:-1]
    (word, uid) = cur_label.split(',')
    print "%s\t%s\t%s" % (word, uid, tag_str)


for line in sys.stdin:
    values = line.strip().split('\t')
    label = values[0]
    hashtags = values[1].split(',')
    
    # same (word, uid) pair, reduce hashtags
    if (label == cur_label):
        for hashtag in hashtags:
            if (hashtag in cur_tags):
                cur_tags[hashtag] += 1
            else:
                cur_tags[hashtag] = 1
                
    # different (word, uid) pair, print result and reinitialize records
    else:
        if (cur_label != None):
            print_result()
        cur_label = label
        cur_tags = dict()
        for hashtag in hashtags:
            if (hashtag in cur_tags):
                cur_tags[hashtag] += 1
            else:
                cur_tags[hashtag] = 1
                
print_result()