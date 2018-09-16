# Reducer code to prepare word table.
# Input of this reducer should be output of prepare_word_table_mapper.py
# Each line of data should have 2 columns: word  hashtags
# Columns are separated by tab, hashtags are separated by ','
# and each tag and its correponding score are separated by ':'

#! /usr/bin/python
import sys

cur_word = None
cur_tags = dict()

# print current record
def print_result():
    tag_str = ""
    for k,v in cur_tags.items():
        tag_str +=  "%s:%d," % (k, v)
    tag_str = tag_str[:-1]
    print "%s\t%s" % (cur_word, tag_str)


for line in sys.stdin:
    values = line.strip().split('\t')
    word = values[0]
    hashtags = values[1].split(',')
    
    # same (word, uid) pair, reduce hashtags
    if (word == cur_word):
        for hashtag in hashtags:
            (tag, count) = hashtag.split(':')
            count = int(count)
            if (tag in cur_tags):
                cur_tags[tag] += count
            else:
                cur_tags[tag] = count
                
    # different (word, uid) pair, print result and reinitialize records
    else:
        if (cur_word != None):
            print_result()
        cur_word = word
        cur_tags = dict()
        for hashtag in hashtags:
            (tag, count) = hashtag.split(':')
            count = int(count)
            if (tag in cur_tags):
                cur_tags[tag] += count
            else:
                cur_tags[tag] = count
                
print_result()