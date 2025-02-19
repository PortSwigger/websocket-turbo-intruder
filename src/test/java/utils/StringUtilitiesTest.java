package utils;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.FakeByteArray;
import burp.api.montoya.core.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static burp.api.montoya.core.FakeRange.rangeOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static utils.Utilities.insertPlaceholder;

public class StringUtilitiesTest {
    private static Stream<Arguments> overlongData() {
        return Stream.of(
                arguments("A", 1, "%41"),
                arguments("/", 2, "%C0%AF")
        );
    }

    private static Stream<Arguments> truncatedData() {
        return Stream.of(
                arguments("AA", "%E4%85%81")
        );
    }

    private static Stream<Arguments> wordData() {
        return Stream.of(
                arguments("The quick brown fox jumps over the lazy dog", 9),
                arguments("Un jugoso zumo de piña y kiwi bien frío es exquisito y no lleva alcohol", 15)
        );
    }

    @ParameterizedTest
    @MethodSource("overlongData")
    void overlongUTF8Test(String input, int length, String expectedOutput)
    {
        String modifiedData = StringUtillities.overlongUTF8(input, length , false);
        assertThat(modifiedData).isEqualTo(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("truncatedData")
    void truncatedUTF8Test(String input, String expectedOutput)
    {
        String modifiedData = StringUtillities.truncatedUTF8(input, false);
        assertThat(modifiedData).isEqualTo(expectedOutput);
    }

    @ParameterizedTest
    @MethodSource("wordData")
    void wordsCountTest(String input, int expectedOutput)
    {
        int words = StringUtillities.countWords(input);
        assertThat(words).isEqualTo(expectedOutput);
    }
}
