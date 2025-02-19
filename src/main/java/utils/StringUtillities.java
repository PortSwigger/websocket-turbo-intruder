package utils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Character.isLetter;

public class StringUtillities {

    public static String truncatedUTF8(String input, boolean unescaped) {
        return unescaped ? truncatedUTF8(input) : URLEncoder.encode(truncatedUTF8(input), Charset.defaultCharset());
    }

    public static String truncatedUTF8(String input) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length() - 1; i += 2) {
            char firstChar = input.charAt(i);
            char secondChar = input.charAt(i + 1);

            int combinedValue = (firstChar * 256) + secondChar;
            String utf8String = Character.toString(combinedValue);
            result.append(utf8String);
        }
        if(input.length() % 2 == 1) result.append(input.charAt(input.length() - 1));
        return result.toString();
    }

    public static String overlongUTF8(String code) {
        return overlongUTF8(code, 2, true);
    }
    public static String overlongUTF8(String code, int length, boolean unescaped) {
        List<String> output = new ArrayList<>();

        for (int i = 0; i < code.length(); i++) {
            int chr = code.charAt(i);
            output.add(convertUTF8(chr, length, unescaped));
        }

        return String.join("", output);
    }

    public static int countWords(String str) {
        if (str == null || str.isEmpty()) {
            return 0; // Return 0 for null or empty strings
        }

        String normalized = Normalizer.normalize(str, Normalizer.Form.NFKC).toLowerCase();

        int wordCount = 0;
        boolean inWord = false;

        for (char c : normalized.toCharArray()) {
            if (isLetter(c)) {
                if (!inWord) {
                    wordCount++; // Start a new word
                    inWord = true;
                }
            } else {
                inWord = false; // End of a word
            }
        }

        return wordCount;
    }

    private static String escapeChar(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(String.format("%%%02X", (int) c));
        }
        return sb.toString();
    }

    private static String escapeChar(char ch) {
        return String.format("%%%02X", (int) ch);
    }

    private static String convertUTF8(int chr, int n, boolean unescaped) {
        if (n < 2) {
            return unescaped ? Character.toString(chr) : escapeChar((char) chr);
        }

        List<Character> chars = new ArrayList<>();
        chars.add((char) ((0x100 - (1 << (8 - n))) | ((1 << (7 - n)) - 1 & (chr >> (6 * (n - 1))))));
        chr %= n < 7 ? 1 << 6 * (n - 1) : (int) Math.pow(2, 6 * (n - 1));

        for (int i = 1; i < n; i++) {
            chars.add((char) (0x80 | (63 & (chr >> 6 * (n - i - 1)))));
            chr %= n < 7 ? 1 << 6 * (n - i - 1) : (int) Math.pow(2, 6 * (n - i - 1));
        }

        StringBuilder result = new StringBuilder();
        for (char ch : chars) {
            result.append(ch);
        }

        return unescaped ? result.toString() : escapeChar(result.toString());
    }

}
