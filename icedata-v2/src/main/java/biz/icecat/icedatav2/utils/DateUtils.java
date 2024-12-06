package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

@Slf4j
@UtilityClass
public class DateUtils {

    public final static ZoneId UTC = ZoneId.of("UTC");

    private final static DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .appendOptional(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            .appendOptional((DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .toFormatter();

    public static LocalDateTime localDateTime(Long dateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime), UTC);
    }

    public static LocalDateTime parse(String dateTimeString) {
        if (StringUtils.isBlank(dateTimeString)) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeString, FORMATTER);
        } catch (Exception e) {
            log.warn("Couldn't produce LocalDateTime out of {}", dateTimeString);
            return null;
        }
    }

    public static Long parseToLong(String dateTimeString) {
        if (StringUtils.isBlank(dateTimeString)) {
            return null;
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, FORMATTER);
            return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            log.warn("Couldn't parse Zoned Date Time out of {}", dateTimeString);
            return Optional.ofNullable(parse(dateTimeString))
                    .map(ldt -> ldt.atZone(UTC).toInstant().toEpochMilli())
                    .orElse(null);
        }
    }

    public static String format(Long timestamp, ZoneId zone) {
        if (timestamp == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), UTC);
        return zonedDateTime.withZoneSameInstant(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
