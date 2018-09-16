import sys
import json
import re
from collections import OrderedDict
from unicode_tr import unicode_tr
from sets import Set
from datetime import datetime
import calendar
import string


# a whitelist for special unicode (keep updating)
SPECIAL_UNIC = [u"\u278a", u"\u278b", u"\u278c", u"\u278d", u"\u278e", u"\u278f", 
    u"\u2776", u"\u2777", u"\u2778", u"\u2779", u"\u2780", u"\u2781", u"\u2782",
    u"\u2460", u"\u2461", u"\u2462", u"\u2463", u"\u2464", u"\u2465", u"\u2466",
    u"\u2467", u"\u2468", u"\u2469", u"\u246A", u"\u246B", u"\u246C", u"\u246D",
    u"\u246E", u"\u246F", u"\u2470", u"\u2471", u"\u2472", u"\u2473", u"\u2474",
    u"\u2475", u"\u2476", u"\u2477", u"\u2478", u"\u2479", u"\u247A", u"\u247B",
    u"\u247C", u"\u247D", u"\u247E", u"\u247F", u"\u2480", u"\u2481", u"\u2482",
    u"\u2483", u"\u2484", u"\u2485", u"\u2486", u"\u2487", u"\u2488", u"\u2489",
    u"\u248A", u"\u248B", u"\u248C", u"\u248D", u"\u248E", u"\u248F", u"\u2490",
    u"\u2491", u"\u2492", u"\u2493", u"\u2494", u"\u2495", u"\u2496", u"\u2497",
    u"\u2498", u"\u2499", u"\u249A", u"\u249B", u"\u249C", u"\u249D", u"\u249E",
    u"\u249F", u"\u24A0", u"\u24A1", u"\u24A2", u"\u24A3", u"\u24A4", u"\u24A5",
    u"\u24A6", u"\u24A7", u"\u24A8", u"\u24A9", u"\u24AA", u"\u24AB", u"\u24AC",
    u"\u24AD", u"\u24AE", u"\u24AF", u"\u24B0", u"\u24B1", u"\u24B2", u"\u24B3",
    u"\u24B4", u"\u24B5", u"\u24B6", u"\u24B7", u"\u24B8", u"\u24B9", u"\u24BA",
    u"\u24BB", u"\u24BC", u"\u24BD", u"\u24BE", u"\u24BF", u"\u24C0", u"\u24C1",
    u"\u24C2", u"\u24C3", u"\u24C4", u"\u24C5", u"\u24C6", u"\u24C7", u"\u24C8",
    u"\u24C9", u"\u24CA", u"\u24CB", u"\u24CC", u"\u24CD", u"\u24CE", u"\u24CF",
    u"\u24D0", u"\u24D1", u"\u24D2", u"\u24D3", u"\u24D4", u"\u24D5", u"\u24D6",
    u"\u24D7", u"\u24D8", u"\u24D9", u"\u24DA", u"\u24DB", u"\u24DC", u"\u24DD",
    u"\u24DE", u"\u24DF", u"\u24E0", u"\u24E1", u"\u24E2", u"\u24E3", u"\u24E4",
    u"\u24E5", u"\u24E6", u"\u24E7", u"\u24E8", u"\u24E9", u"\u24EA", u"\u24EB",
    u"\u24EC", u"\u24ED", u"\u24EE", u"\u24EF", u"\u24F0", u"\u24F1", u"\u24F2",
    u"\u24F3", u"\u24F4", u"\u24F5", u"\u24F6", u"\u24F7", u"\u24F8", u"\u24F9",
    u"\u24FA", u"\u24FB", u"\u24FC", u"\u24FD", u"\u24FE", u"\u24FF", u"\u3021",
    u"\u00b2", u"\u2783", u"\u277a", u"\u2776", u"\u2777", u"\u2778", u"\u2779",
    u"\u277b", u"\u277c",u"\u277d", u"\u277e", u"\u2783", u"\u2784", u"\u2785",
    u"\u2786", u"\u2787", u"\u2788", u"\u2789", u"\u2790", u"\u2791", u"\u2792",
    u"\u2793", u"\u2070", u"\u00b9", u"\u00bb", u"\u00bc", u"\u00bd", u"\u00be",
    u"\u00bf", u"\u2160", u"\u2161", u"\u2162", u"\u2163", u"\u2164", u"\u2165",
    u"\u2166", u"\u2167", u"\u2168", u"\u2169", u"\u216a", u"\u216b"]

STOP_WORDS = Set()
CENSOR_WORDS = Set()
rot13 = string.maketrans("ABCDEFGHIJKLMabcdefghijklmNOPQRSTUVWXYZnopqrstuvwxyz",
    "NOPQRSTUVWXYZnopqrstuvwxyzABCDEFGHIJKLMabcdefghijklm")

def run(fileout):
    """
    Main function used to run the pipeline
    1. read part-xxxxx
    2. run ETL
    3. save information on output
    """

    stop_words_file = open("stopwords.txt")
    lines = stop_words_file.readlines()
    for line in lines:
        STOP_WORDS.add(line.strip())

    censor_words_file = open("censorwords.txt")
    lines = censor_words_file.readlines()
    for line in lines:
        CENSOR_WORDS.add(string.translate(line.strip(), rot13))

    out = open(fileout, 'wt')
    for line in sys.stdin:
        lines = json.loads(line)
        status, normalized = set_filters(lines)
        #print(normalized)
        if status == True:
            out.write(normalized.encode('utf-8') + "\n")

def extract_words(text):
    #print text
    word_list_all = re.findall(ur'(?:[A-Za-z0-9\'\-])+', text)
    word_list = []
    for word in word_list_all:
        word = word.lower()
        if word in STOP_WORDS:
            continue
        has_letter = False
        for c in word:
            if c.isalpha():
                has_letter = True
                break
        if (has_letter):
            word_list.append(word)
    return word_list

def convert_time(created_at):
    dt = datetime.strptime(created_at, '%a %b %d %H:%M:%S +0000 %Y')
    return str(calendar.timegm(dt.timetuple()))

def censor_text(text):
    words = Set(re.findall(ur'([A-Za-z0-9]+)', text, flags=re.U))
    for word in words:
        if word.lower() in CENSOR_WORDS:
            censored_word = ""
            censored_word += word[0]
            for i in range (1, len(word)-1):
                censored_word += "*"
            censored_word += word[-1]
            text = re.sub(r"(?<![A-Za-z0-9])%s(?![A-Za-z0-9])" % word, censored_word, text, flags=re.U)
    return text


def set_filters(line):
    """
    Filter records and store the required fields if the records
    contain all required fields
    """
    data = {}

    # if both id and id_str of the tweet objects are missing or empty
    if (filter_helper(line, "id_str") or filter_helper(line, "id")) == False:
        return False, ""
    else:
        data['tweet_id'] = line['id_str'] ## todo, decide which one

    # if "created_at" field is missing or empty
    if filter_helper(line, "created_at") == False:
        return False, ""
    else:
        unix_time = convert_time(line["created_at"])
        data["created_at"] = unix_time

    # if both id and id_str in users object are missing or empty
    if filter_helper(line, "user") == False:
        return False, ""
    else:
        user_profile = line["user"]
        if (filter_helper(user_profile, "id_str") or filter_helper(user_profile, "id")) == False:
            return False, ""
        else :
            data['user_id'] = str(user_profile["id"])


    # if "lang" field is missing or not en
    if (filter_helper(line, "lang") == False) or line["lang"] != "en":
        return False, ""


    # if "text" field is missing or empty
    if filter_helper(line, "text") == False:
        return False, ""


    # remove the url and special unicode
    text_orig = unicode_tr(line['text'])
    data['censored_text'] = censor_text(text_orig)

    # split the text field
    text_lower = shorten_url(text_orig)
    word_list = extract_words(text_lower)
    data["impactScore"] = str(calculate_impact_score(line, word_list))


    return True, json.dumps(OrderedDict([("tweet_id", data["tweet_id"]),
        ("created_at", data["created_at"]), ("user_id", data["user_id"]),
        ("censored_text", data["censored_text"]), ("impactScore", data["impactScore"])]),
        ensure_ascii=False, separators=(',', ':'), sort_keys=False)


def calculate_impact_score(line, word_list):
    favorite_count = line["favorite_count"]
    if favorite_count == None:
        favorite_count = 0
    else :
        favorite_count = int(favorite_count)

    retweet_count = line["retweet_count"]
    if retweet_count == None:
        retweet_count = 0
    else :
        retweet_count = int(retweet_count)

    followers_count = line["user"]["followers_count"]
    if followers_count == None:
        followers_count = 0
    else :
        followers_count = int(followers_count)

    return max(0, len(word_list) * (followers_count + retweet_count + favorite_count))



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


def shorten_url(input_str):
    """
    Regex to the urls and remove such shorten urls
    """
    pattern = r'(https?|ftp):\/\/[^\t\r\n /$.?#][^\t\r\n ]*'
    return re.sub(pattern, '', input_str)



if __name__ == "__main__":
    """
    Change to False if you need to remove the stop words
    """
    run("OUTPUT")
