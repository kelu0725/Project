import sys
import json
import re
from collections import OrderedDict
from unicode_tr import unicode_tr


def run():
    """
    Main function used to run the pipeline
    1. read part-xxxxx
    2. run ETL
    3. save information on output
    """

    for line in sys.stdin:
        lines = json.loads(line)
        status, data = set_filters(lines)
        if status == True:
            values = [
                        data["tweet_id"],
                        data["timestamp"],
                        data["user_id"],
                        json.dumps(data["user_name"])[1:-1],
                        str(data["favorite_count"]),
                        str(data["retweet_count"]),
                        json.dumps(data["text"])[1:-1]
                      ]
            output = "\t".join(values)
            print output.encode('utf-8')


def set_filters(line):
    """
    Filter records and store the required fields if the records
    contain all required fields
    """
    data = {}

    # if both id and id_str of the tweet objects are missing or empty
    if filter_helper(line, "id_str") == True:
        data['tweet_id'] = line['id_str'] 
    elif filter_helper(line, "id") == True:
        data['tweet_id'] = line['id']
    else:
        return False, ""

    # if "created_at" field is missing or empty
    if filter_helper(line, "created_at") == False:
        return False, ""
    else:
        data["timestamp"] = line["created_at"]

    # if both id and id_str in users object are missing or empty
    if filter_helper(line, "user") == False:
        return False, ""
    
    user_profile = line["user"]
    if (filter_helper(user_profile, "id_str") == True):
        data['user_id'] = str(user_profile["id_str"])
    elif (filter_helper(user_profile, "id") == True):
        data['user_id'] = str(user_profile["id_str"])
    else:
        return False, ""
          
    if (filter_helper(user_profile, "screen_name") == False):
        return False, ""
    else :        
        data['user_name'] = unicode_tr(user_profile["screen_name"])


    # if "lang" field is missing or not en
    if (filter_helper(line, "lang") == False) or line["lang"] != "en":
        return False, ""

    # if "text" field is missing or empty
    if filter_helper(line, "text") == False:
        return False, ""
    else:
        data["text"] = unicode_tr(line["text"])

    # if "favorite_count" field is missing or empty
    if filter_helper(line, "favorite_count") == False:
        return False, ""
    else:
        data["favorite_count"] = line["favorite_count"]
    
    # if "favorite_count" field is missing or empty
    if filter_helper(line, "retweet_count") == False:
        return False, ""
    else:
        data["retweet_count"] = line["retweet_count"]

    return True, data


def filter_helper(line, field):
    """
    Helper function used to check whether the field exists
    or is empty
    """
    # no such field
    if (field in line) is False:
        return False
    # empty field
    if (line[field] == []) is True:
        return False

    return True



if __name__ == "__main__":
    """
    Change to False if you need to remove the stop words
    """
    run()
