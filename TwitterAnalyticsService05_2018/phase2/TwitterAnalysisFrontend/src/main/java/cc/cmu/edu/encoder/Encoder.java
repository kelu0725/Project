package cc.cmu.edu.encoder;


public class Encoder {
	/*
	 * Length of single byte
	 */
	private static final int BYTE_LENGTH = 8;
	/*
	 * Size of version 1 QR code.
	 */
	private static final int V1_SIZE = 21;
	/*
	 * Size of version 2 QR code.
	 */
	private static final int V2_SIZE = 25;
	/*
	 * String to append at the tail of payload.
	 */
	private static final String APPEND_STR = "1110110000010001";
	/*
	 * Version 1 QR code template. 0 and 1 represents patterns, 2 represents empty cells.
	 */
	private static String v1_template = "111111102222201111111"
			+ "100000102222201000001101110102222201011101"
			+ "101110102222201011101101110102222201011101"
			+ "100000102222201000001111111101010101111111"
			+ "000000002222200000000222222122222222222222"
			+ "222222022222222222222222222122222222222222"
			+ "222222022222222222222222222122222222222222"
			+ "000000002222222222222111111102222222222222"
			+ "100000102222222222222101110102222222222222"
			+ "101110102222222222222101110102222222222222"
			+ "100000102222222222222111111102222222222222";
	/*
	 * Version 2 QR code template. 0 and 1 represents patterns, 2 represents empty cells.
	 */
	private static String v2_template = "1111111022222222201111111"
			+ "10000010222222222010000011011101022222222201011101"
			+ "10111010222222222010111011011101022222222201011101"
			+ "10000010222222222010000011111111010101010101111111"
			+ "00000000222222222000000002222221222222222222222222"
			+ "22222202222222222222222222222221222222222222222222"
			+ "22222202222222222222222222222221222222222222222222"
			+ "22222202222222222222222222222221222222222222222222"
			+ "22222202222222222222222222222221222222222111112222"
			+ "00000000222222221000122221111111022222222101012222"
			+ "10000010222222221000122221011101022222222111112222"
			+ "10111010222222222222222221011101022222222222222222"
			+ "10000010222222222222222221111111022222222222222222";

	public String encode(String msg) {
		String payload = getPayloadArray(msg);
		StringBuilder bar_code = fillBarCode(msg, payload);
		// get 32-bit integer representation of barcode
		String output = getIntegerArray(bar_code.toString());
		return output;
	}

	
	/**
	 * Transform integer to its binary representation with one byte length.
	 * @param n integer input
	 * @return binary representation of n
	 */
	private String getBinaryByte(int n) {
		StringBuilder byte_array = new StringBuilder(Integer.toBinaryString(n));
		int len = byte_array.length();
		// Append 0s at front
		for (int i = len; i < BYTE_LENGTH; i++) {
			byte_array.insert(0, '0');
		}
		return byte_array.toString();
	}
	
	/**
	 * Transform each character of message to its binary representation with error code.
	 * @param c char input
	 * @return binary representation of c
	 */
	private String getMsgPayload(String msg) {
		StringBuilder msg_array = new StringBuilder();
		for (int i = 0; i < msg.length(); i++) {
			// get binary representation of character
			String char_byte = getBinaryByte(msg.charAt(i));
			msg_array.append(char_byte);
			// get error code of character
			int error_code = 0;
			for (int j = 0; j < char_byte.length(); j++) {
				error_code += char_byte.charAt(j) - '0';
			}
			msg_array.append(getBinaryByte(error_code % 2));
		}
		return msg_array.toString();
	}
	
	/**
	 * Get payload array of message.
	 * @param msg message input
	 * @return binary representation payload of message
	 */
	private String getPayloadArray(String msg) {
		StringBuilder payload = new StringBuilder();
		
		// add length of message to payload
		payload.append(getBinaryByte(msg.length()));
		// add each character and its error code to payload
		payload.append(getMsgPayload(msg));
		
		return payload.toString();
	}
	
	/**
	 * Transform message into a 2D barcode represented by a binary array.
	 * @param msg original message
	 * @param payload binary payload array of message
	 * @return barcode of message
	 */
	private StringBuilder fillBarCode(String msg, String payload) {
		// get template
		int len = msg.length(), size = 0;
		StringBuilder bar_code;
		if (len <= 14) {
			bar_code = new StringBuilder(v1_template);
			size = V1_SIZE;
		} else {
			bar_code = new StringBuilder(v2_template);
			size = V2_SIZE;
		}
		
		// fill in payload
		int pos = 0;
		String str = new String(payload);
		// from right bottom
		for (int c = size - 1; c >= 0; c--) {
			// even column, fill from bottom to top
			if (c % 2 == 0) {
				for (int r = size - 1; r >= 0; r--) {
					int index = r * size + c;
					if (bar_code.charAt(index) != '2') {
						continue;
					}
					if (pos >= str.length()) {
						pos = 0;
						str = new String(APPEND_STR);
					}
					bar_code.setCharAt(index, str.charAt(pos));
					pos++;
				}
			} 
			// odd column, fill from top to bottom
			else {
				for (int r = 0; r < size; r++) {
					int index = r * size + c;
					if (bar_code.charAt(index) != '2') {
						continue;
					}
					if (pos >= str.length()) {
						pos = 0;
						str = new String(APPEND_STR);
					}
					bar_code.setCharAt(index, str.charAt(pos));
					pos++;
				}
			}
		}
		return bar_code;
	}
	
	/**
	 * Transform barcode binary array to hex representation of 32-bit integers.
	 * @param bar_code binary barcode
	 * @return 32-bit integer representation of barcode
	 */
	private String getIntegerArray(String bar_code) {
		int int_len = 32;
		StringBuilder output = new StringBuilder(); 
		int i = 0;
		for (i = 0; i <= bar_code.length() - int_len; i += int_len) {
			int n = 0;
			if (bar_code.charAt(i) == '1') {
				n = Integer.MIN_VALUE;
			}
			for (int j = 1; j < int_len; j++) {
				if (bar_code.charAt(i + j) == '1') {
					n += (int) Math.pow(2, int_len - j - 1);
				}
			}
			String hex_n = Integer.toHexString(n);
			output.append("0x" + hex_n);
		}
		// treat left bits as a single integer
		int n = Integer.parseInt(bar_code.substring(i), 2);
		String hex_n = Integer.toHexString(n);
		output.append("0x" + hex_n);
		return output.toString();
	}
}
