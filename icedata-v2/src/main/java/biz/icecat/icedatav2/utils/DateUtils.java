package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@UtilityClass
public class DateUtils {

    private final static DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendOptional((DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toFormatter();

    public static LocalDateTime localDateTime(Long dateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime), ZoneId.of("UTC"));
    }

    public static LocalDateTime parse(String dateTimeString) {
        if (StringUtils.isBlank(dateTimeString)) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeString, FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported date format: " + dateTimeString, e);
        }
    }
}
