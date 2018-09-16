import sys
import json
import re
from collections import OrderedDict
from unicode_tr import unicode_tr
from stop_words import get_stop_words
import argparse

# the vaild language sets in this case
AVALABLE_LANG = ["ar", "en", "fr", "in", "pt", "es", "tr"]
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


# create parser
def create_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', help='the name of output file')
    parser.add_argument('--rm', action='store_true', default=False, help='remove the stop words in the text fields')
    return parser


def run():
	"""
	Main function used to run the pipeline
	1. read part-xxxxx
	2. run ETL 
	3. save information on output
	"""
	# parse the arguments
	args = create_parser().parse_args()
	out = open(args.output, 'wt')
	for line in sys.stdin:
		lines = json.loads(line)
		status, normalized = set_filters(lines, args.rm)
		if status == True:
			output = format_transform(json.loads(normalized))
			out.write(output.encode('utf-8') + "\n") 


def format_transform(data):
	"""
	Convert to tsv format
	"""
	str1 = [data["uid"], ",".join(data["hashtags"]), data["text"], data["tid"]]
	return "\t".join(str1)

def set_filters(line, is_remove_sw):
	"""
	Filter records and store the required fields if the records
	contain all required fields
	"""
	data = {}
	lang = ""
	lang_user = ""

	# if both id and id_str in users object are missing or empty
	if filter_helper(line, "user") == False:
		return False, ""
	else:
		user_profile = line["user"]
		if (filter_helper(user_profile, "id_str") or filter_helper(user_profile, "id")) == False:
			return False, ""
		else :
			data['uid'] = str(user_profile["id"])
		if (filter_helper(user_profile, "lang")):
			lang_user = user_profile["lang"]

	# if hashtag in entities is missing or empty
	if filter_helper(line, "entities") == False:
		return False, ""
	else:
		hashtag_list = line["entities"]
		if filter_helper(hashtag_list, "hashtags"):
			data['hashtags'] = []
			for ele in hashtag_list['hashtags']:
				data['hashtags'].append(ele['text'])
		else:
			return False, ""

	# if "lang" field is missing or not in the list
	if filter_helper(line, "lang") == False:
		return False, ""
	elif line['lang'] in AVALABLE_LANG :
		lang = line['lang']
	else :
		return False, ""

	# if "text" field is missing or empty
	if filter_helper(line, "text") == False:
		return False, ""
	else :
		# remove the url and special unicode
		output1 = shorten_url(line['text'])
		output1 = clean_ar_text(output1)

		# split the text field
		ele_list = re.findall(ur'(?:[^\W\d_])+', output1, flags=re.U | re.I)
		text_str = ",".join([x for x in ele_list if x not in SPECIAL_UNIC])

		# special attention on i (note that based on comparing with reference,
		# it is not only applied to tr lang set)
		data['text'] = text_str.replace(u"\u0130", u"i\u0307")
		data['text'] = data['text'].replace("I", "i")
		data['text'] = unicode_tr(data['text']).lower()

		# note that Indonesian stoplist is not available for now
		if lang != "in" and is_remove_sw:
			data['text'] = remove_stop_words(data['text'], lang)

	# if both id and id_str of the tweet objects are missing or empty
	if (filter_helper(line, "id_str") or filter_helper(line, "id")) == False:
		return False, ""
	else :
		data['tid'] = line['id_str'] ## todo, decide which one 

	# if "created_at" field is missing or empty
	if filter_helper(line, "created_at") == False:
		return False, ""

	return True, json.dumps(OrderedDict([("uid", data["uid"]), ("hashtags", data["hashtags"]),
		("text", data["text"]), ("tid", data["tid"])]), 
		ensure_ascii=False, separators=(',', ':'), sort_keys=False)


def clean_ar_text(text):
	"""
	Omit the special unicode in text; the current whitelist may not
	be completed
	"""
	for rgx_match in SPECIAL_UNIC:
		text = text.replace(rgx_match, ',')
	return text


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


def remove_stop_words(str1, lang):
	"""
	Used for the further analysis; remove the stopword
	"""
	stop_words = get_stop_words(lang)
	str_list = str1.split(",")
	return ",".join([x for x in str_list if x not in stop_words])


if __name__ == "__main__":
	run()
