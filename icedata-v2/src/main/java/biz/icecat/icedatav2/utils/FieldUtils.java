package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class FieldUtils {
    public static Function<String, Long> mapToLong = (string) -> {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            return null;
        }
    };

    public static Function<String, Integer> mapToInt = (string) -> {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    };

    public static Function<String, Integer> mapToIntOrZero = mapToInt.andThen(num -> num != null ? num : 0);
}
