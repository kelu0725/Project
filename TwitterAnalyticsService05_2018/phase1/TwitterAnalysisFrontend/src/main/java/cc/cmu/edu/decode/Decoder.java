package cc.cmu.edu.decode;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.cmu.edu.decode.Preprocess;

public class Decoder {
	/**
	 * A String[] that is used to store the binary format of 
	 * the requested string
	 */
	public String[] input_string;
	/**
	 * The size of valid QR area
	 */
	private int qr_size;
	/**
	 * Length of single byte
	 */
	private static final int BYTE_LENGTH = 8;
	
	/**
	 * Use regex in java to find the location of QR code
	 * @return a String that is used to store the valid message in QR code
	 */
	public String extract_qr() {
		// Coarse match; note that it is specific to V1 and V2 
		// modify the regex pattern if more versions are involved
		String regex = "11111110101010(1010)?111111"; 
		
		for (int i = 0; i < input_string.length; i ++) {
			Pattern ptn = Pattern.compile(regex);
			Matcher matcher = ptn.matcher(input_string[i]);
			if (matcher.find()) {
				// get the size of valid QR code
				int start = matcher.start();
				int end = matcher.end();
				qr_size = end - start + 1;
				//System.out.println(start);
				//System.out.println(qr_size);
				// run ex
				if (qr_size == 21 || qr_size == 25) {
					String output = exact_match(i, start, end);
					if (output != null) {
						//System.out.println(output);
						return output;
					}
				}
			}
		}
		
		// exit when no match can be found
		System.out.println("Not a valid V1 or V2 QR code.");
		//System.exit(0);
		return null;
	}
	
	
	/**
	 * extract_match is used to fine-match the candidates and return the  
	 * payload if it is a hit. More precisely, after taking the column location, 
	 * the position of start and end from the coarse match, we will consider four
	 * possible cases and a exact match. 
	 * 
	 * @param col_loci The column index of the coarse match string
	 * @param start The start loci of the coarse match string
	 * @param end The end loci of the coarse match string
	 * @return a String that stores the data codeword in the QR code
	 */
	private String exact_match(int col_loci, int start, int end) {
		// initialize the variables
		String regex = "00000000";
		int dist = qr_size - 7; // 25 : dist = 11+7; 21 : dist = 7+7
		String upper_left = "";
		String upper_right = "";
		String lower_left = "";
		String lower_right = "";
		
		//System.out.println(lower_left);
		//System.out.println(lower_right);
		
		// check whether the four possible cases are valid
		if (col_loci - dist >= 0) {
			upper_left = input_string[col_loci - dist + 7].substring(start, start+8);
			upper_right = input_string[col_loci - dist + 7].substring(end-7, end+1);
		}
		if (col_loci + dist < 32) {
			lower_left = input_string[col_loci + dist - 7].substring(start, start+8);
			lower_right = input_string[col_loci + dist - 7].substring(end-7, end+1);
		}
		
		//System.out.println(lower_left);
		//System.out.println(lower_right);
		RegexPattern rp = new RegexPattern(qr_size);
		String general_regex_pattern = rp.get_pattern();
		
		/* if the three position detection patterns are located like
		 *   PD
		 *   PD   PD
		 *   check the match
 		 */
		if (upper_left.equals(regex)) {
			Pattern ptn = Pattern.compile(rp.get_pattern_rotate270());
			String extracted_string = get_output(start, end, col_loci-dist-1, col_loci + 7-1);
			Matcher matcher = ptn.matcher(extracted_string);
			if (matcher.find()) {
				return extractMsgBinaryArray(rotate90(extracted_string), general_regex_pattern);
			}
		}
		
		/* if the three position detection patterns are located like
		 *   PD	  PD
		 *   PD   
		 *   check the match
 		 */
		if (lower_left.equals(regex)) {
			Pattern ptn = Pattern.compile(rp.get_pattern());
			String extracted_string = get_output(start, end, col_loci-7, col_loci + dist);
			Matcher matcher = ptn.matcher(extracted_string);
			if (matcher.find()) {
				return extractMsgBinaryArray(extracted_string, general_regex_pattern);
			}
		}
		
		/* if the three position detection patterns are located like
		 *   	  PD
		 *   PD   PD
		 *   check the match
 		 */
		if (upper_right.equals(regex)) {
			Pattern ptn = Pattern.compile(rp.get_pattern_rotate180());
			String extracted_string = get_output(start, end, col_loci-dist - 1, col_loci + 7-1);
			
			Matcher matcher = ptn.matcher(extracted_string);
			if (matcher.find()) {
				return extractMsgBinaryArray(rotate90(rotate90(extracted_string)), general_regex_pattern);
			}
		}
		
		/* if the three position detection patterns are located like
		 *   PD	  PD
		 *        PD
		 *   check the match
 		 */
		if (lower_right.equals(regex)) {
			Pattern ptn = Pattern.compile(rp.get_pattern_rotate90());
			String extracted_string = get_output(start, end, col_loci-7, col_loci + dist);
			Matcher matcher = ptn.matcher(extracted_string);
			if (matcher.find()) {
				return extractMsgBinaryArray(rotate90(rotate90(rotate90(extracted_string))), general_regex_pattern);
			}
		}
		return null;
	}
	
	/**
	 * extractMsgBinaryArray function takes the complete QR code and its pattern template
	 * and returns the payload information in the QR code.
	 * @param bar_code a complete QR code
	 * @param template its pattern template
	 * @return the payload information in the QR code
	 */
	private String extractMsgBinaryArray(String bar_code, String template) {
		StringBuilder bin_array = new StringBuilder();

		// extract binary array of message
		for (int c = qr_size - 1; c >= 0; c--) {
			// from bottom to top
			if (c % 2 == 0) {
				for (int r = qr_size - 1; r >= 0; r--) {
					int index = r * qr_size + c;
					if (template.charAt(index) == '.') {
						bin_array.append(bar_code.charAt(index));
					}
				}
			}
			// from top to bottom
			else {
				for (int r = 0; r < qr_size; r++) {
					int index = r * qr_size + c;
					if (template.charAt(index) == '.') {
						bin_array.append(bar_code.charAt(index));
					}
				}
			}
		}
		return bin_array.toString();
	}
	
	/**
	 * decodeMessages() takes the payload and return the decoded message.
	 * @param bin_array the payload
	 * @return the decoded message
	 */
	public static String decodeMessage(String bin_array) {
		// get message length
		int len = Integer.parseInt(bin_array.substring(0, BYTE_LENGTH), 2);
		StringBuilder msg = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int start_pos = (i*2 + 1) * BYTE_LENGTH;
			msg.append((char)Integer.parseInt(bin_array.substring(start_pos, start_pos + BYTE_LENGTH), 2));
		}
		
		return msg.toString();
	}
	
	/**
	 * get_output() is used to extract a valid QR code from the given string.
	 * More precisely, extract a 25*25 or 21*21 from 32*32. 
	 * @param r1 the lower bound of row in the valid QR code
	 * @param r2 the upper bound of row in the valid QR code
	 * @param c1 the lower bound of column in the valid QR code
	 * @param c2 the upper bound of column in the valid QR code
	 * @return extracted QR code in the form of String
	 */ 
	private String get_output(int r1, int r2, int c1, int c2) {
		StringBuilder output = new StringBuilder();
		//System.out.println(r1 +"\t" + r2 + "\t" + c1 + "\t"+c2);
		for (int i = c1+1; i <= c2; i ++) {
			//System.out.println(i + "\t" + input_string[i]);
			output.append(input_string[i].substring(r1, r2+1));
		}
		return output.toString();
	}
	
	/**
	 * rotate90() is used to rotate 90 degree for the given String.
	 * @param in the current QR code
	 * @return  rotated String
	 */
	private String rotate90(String in) {
		StringBuilder out = new StringBuilder(in);
		int size = (int) Math.sqrt(in.length());
		for (int r = 0; r < size; r++) {
			for (int c = 0; c < size; c++) {
				int index_in = r * size + c, index_out = c * size + (size - r - 1);
				out.setCharAt(index_out, in.charAt(index_in));
			}
		}
		return out.toString();
	}
	
	/**
	 * Decode class takes a hex String and returns its stored message.
	 * @param args a hex String
	 */
	public String decode(String decode_str) {
		input_string = new Preprocess(decode_str).run();
		String extracted_qr = extract_qr();
		if (extracted_qr != null) {
			String decode_message = decodeMessage(extracted_qr);
			return decode_message;
		} else {
			System.out.println("Failed to decode...");
			return null;
		} 
	}
}


/**
 * The second class used to modularize the Preprocessing steps.
 *
 */
class Preprocess {
	public String input_string;
	private static final int FULL_BITS = 8;
	private final String PADDING = "0";
	
	/**
	 * preprocess constructor, used to assign the string
	 * that need to be preprocessed
	 * @param input a input string
	 */
	public Preprocess(String input) {
		input_string = input;
	}
	
	/**
	 * Run is used to run preprocessing on the requested string, 
	 * including split into array, transfer to binary representation,
	 *  and fill to 32 bytes.
	 * @return
	 */
	public String[] run() {
		// step1: split based on 0x
		String[] elements = input_string.split("0x");
		String[] output = new String[32];
		int count = 0;
		
		for (int i = 1; i < elements.length; i++) {
			// step2: transfer hex to binary
			String new_ele = hexToBin(elements[i]);
			
			// step2: fill to 32 bytes
			new_ele = new String(new char[FULL_BITS*4 - new_ele.length()]).replace("\0", PADDING) 
					+ new_ele;
			
			// step3: transfer hex to binary
			output[count] = new_ele;
			count += 1;
		}
		return output;
	}
	
	/**
	 * Helper function to convert hex string to binary string
	 * @param s a hex string
	 * @return a binary string
	 */
	private static String hexToBin(String s) {
		return new BigInteger(s, 16).toString(2);
	}
}

