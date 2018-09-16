package cc.cmu.edu.decode;

/**
 * A final class used to store and retrieve the regex pattern for QR code.
 *
 */
public final class RegexPattern {
	
	/**
	 * the regex pattern for the V1 QR code
	 */
	private static final String v1_template = "11111110.....01111111"
			+ "10000010.....0100000110111010.....01011101"
			+ "10111010.....0101110110111010.....01011101"
			+ "10000010.....01000001111111101010101111111"
			+ "00000000.....00000000......1.............."
			+ "......0....................1.............."
			+ "......0....................1.............."
			+ "00000000.............11111110............."
			+ "10000010.............10111010............."
			+ "10111010.............10111010............."
			+ "10000010.............11111110.............";

	/**
	 * the regex pattern for the V2 QR code
	 */
	private static final String v2_template = "11111110.........01111111"
				  + "10000010.........01000001"
				  + "10111010.........01011101"
				  + "10111010.........01011101"
				  + "10111010.........01011101"
				  + "10000010.........01000001"
				  + "1111111010101010101111111"
				  + "00000000.........00000000"
				  + "......1.................."
				  + "......0.................."
				  + "......1.................."
				  + "......0.................."
				  + "......1.................."
				  + "......0.................."
				  + "......1.................."
				  + "......0.................."
				  + "......1.........11111...."
				  + "00000000........10001...."
				  + "11111110........10101...."
				  + "10000010........10001...."
				  + "10111010........11111...."
				  + "10111010................."
				  + "10111010................."
				  + "10000010................."
				  + "11111110.................";
	
	/**
	 * Current pattern
	 */
	private static String current_pattern;
	
	/**
	 * The constructor is used to assign value to the current_pattern
	 * given the size of input.
	 * @param size the size of current pattern
	 */
	public RegexPattern(int size) {
		if (size == 21) {
			current_pattern = v1_template;
		}
		else if (size == 25){
			current_pattern = v2_template;
		} else {
			System.out.println("Invalid Regex Pattern at RegexPattern class.");
			System.exit(0);
		}
	}
	
	/**
	 * rotate90() is used to rotate 90 degree for the given String.
	 * @param in the current QR code
	 * @return  rotated String
	 */
	private String rotate90(String in) {
		StringBuilder out = new StringBuilder(current_pattern);
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
	 * the regular regex pattern
	 * @return the regular regex pattern
	 */
	public String get_pattern() {
		return current_pattern;
	}
	
	/**
	 * the regex pattern that rotates 90 degree
	 * @return the regex pattern that rotates 90 degree
	 */
	public String get_pattern_rotate90() {
		return rotate90(current_pattern);
	}
	
	/**
	 * the regex pattern that rotates 180 degree
	 * @return the regex pattern that rotates 180 degree
	 */
	public String get_pattern_rotate180() {
		return rotate90(rotate90(current_pattern));
	}
	
	/**
	 * the regex pattern that rotates 270 degree
	 * @return the regex pattern that rotates 270 degree
	 */
	public String get_pattern_rotate270() {
		return rotate90(rotate90(rotate90(current_pattern)));
	}	
}
