package com.rockit.ip.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtils {

	public static String[] tokenizeToStringArray(String str, String delimiters) {

		return tokenizeToStringArray(str, delimiters, true, true);
	}
	
	public static String[] tokenizeToStringArray(String str, 
			String delimiters, 
			boolean trimTokens, 
			boolean ignoreEmptyTokens) {



		if (str == null) {

			return new String[0];

		}



		StringTokenizer st = new StringTokenizer(str, delimiters);

		List<String> tokens = new ArrayList<>();

		while (st.hasMoreTokens()) {

			String token = st.nextToken();

			if (trimTokens) {

				token = token.trim();

			}

			if (!ignoreEmptyTokens || token.length() > 0) {

				tokens.add(token);

			}

		}

		return toStringArray(tokens);

	}	
	
	public static String[] toStringArray(Collection<String> collection) {

		return collection.toArray(new String[collection.size()]);

	}	
	
	public static String[] commaDelimitedListToStringArray(String str) {

		return delimitedListToStringArray(str, ",");

	}	
	
	public static String[] delimitedListToStringArray(String str, String delimiter) {

		return delimitedListToStringArray(str, delimiter, null);

	}
	

	public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {



		if (str == null) {

			return new String[0];

		}

		if (delimiter == null) {

			return new String[] {str};

		}



		List<String> result = new ArrayList<>();

		if ("".equals(delimiter)) {

			for (int i = 0; i < str.length(); i++) {

				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));

			}

		}

		else {

			int pos = 0;

			int delPos;

			while ((delPos = str.indexOf(delimiter, pos)) != -1) {

				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));

				pos = delPos + delimiter.length();

			}

			if (str.length() > 0 && pos <= str.length()) {

				// Add rest of String, but not in case of empty input.

				result.add(deleteAny(str.substring(pos), charsToDelete));

			}

		}

		return toStringArray(result);

	}
	
	public static String deleteAny(String inString, String charsToDelete) {

		if (!hasLength(inString) || !hasLength(charsToDelete)) {

			return inString;

		}



		StringBuilder sb = new StringBuilder(inString.length());

		for (int i = 0; i < inString.length(); i++) {

			char c = inString.charAt(i);

			if (charsToDelete.indexOf(c) == -1) {

				sb.append(c);

			}

		}

		return sb.toString();

	}
	
	public static boolean hasLength(CharSequence str) {

		return (str != null && str.length() > 0);

	}
	
	public static String replace(String inString, String oldPattern, String newPattern) {

		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {

			return inString;

		}

		int index = inString.indexOf(oldPattern);

		if (index == -1) {

			// no occurrence -> can return input as-is

			return inString;

		}



		int capacity = inString.length();

		if (newPattern.length() > oldPattern.length()) {

			capacity += 16;

		}

		StringBuilder sb = new StringBuilder(capacity);



		int pos = 0;  // our position in the old string

		int patLen = oldPattern.length();

		while (index >= 0) {

			sb.append(inString.substring(pos, index));

			sb.append(newPattern);

			pos = index + patLen;

			index = inString.indexOf(oldPattern, pos);

		}



		// append any characters to the right of a match

		sb.append(inString.substring(pos));

		return sb.toString();

	}

	
}
