package biz.icecat.icedatav2.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Slf4j
@UtilityClass
public class DateUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)
    );

    public static LocalDateTime localDateTime(Long dateTime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime), UTC);
    }

    /** Normalize timestamps with an explicit offset to UTC; unzoned input is UTC. */
    public static LocalDateTime parse(String dateTimeString) {
        Long timestamp = parseToLong(dateTimeString);
        return timestamp == null ? null : localDateTime(timestamp);
    }

    public static Long parseToLong(String dateTimeString) {
        if (StringUtils.isBlank(dateTimeString)) {
            return null;
        }
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                TemporalAccessor parsedDateTime = formatter.parseBest(dateTimeString, ZonedDateTime::from, LocalDateTime::from);
                Instant instant = toInstant(parsedDateTime);
                return instant.toEpochMilli();
            } catch (DateTimeParseException ignored) {
                // Try the next supported Icecat/API timestamp format.
            }
        }
        log.warn("Couldn't parse timestamp {}", dateTimeString);
        return null;
    }

    private static Instant toInstant(TemporalAccessor parsedDateTime) {
        if (parsedDateTime instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toInstant();
        }
        return ((LocalDateTime) parsedDateTime).toInstant(ZoneOffset.UTC);
    }

    public static String format(Long timestamp, ZoneId zone) {
        if (timestamp == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), UTC);
        return zonedDateTime.withZoneSameInstant(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
