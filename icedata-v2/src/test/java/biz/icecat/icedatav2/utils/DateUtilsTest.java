package biz.icecat.icedatav2.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-02T03:04:05", "2024-01-02 03:04:05", "2024-01-02T03:04:05Z",
            "2024-01-02T06:04:05+03:00", "2024-01-01T22:04:05-05:00",
            "2024-01-02T04:04:05+01:00[Europe/Paris]"})
    void timestampsRepresentTheSameInstant(String source) {
        long expected = Instant.parse("2024-01-02T03:04:05Z").toEpochMilli();
        assertThat(DateUtils.parseToLong(source)).isEqualTo(expected);
        assertThat(DateUtils.parse(source)).isEqualTo(DateUtils.localDateTime(expected));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "garbage", "2024-02-30T00:00:00", "2024-13-01 00:00:00"})
    void invalidOrMissingDatesRemainMissing(String source) {
        assertThat(DateUtils.parseToLong(source)).isNull();
        assertThat(DateUtils.parse(source)).isNull();
    }

    @Test
    void formattingPreservesInstantAcrossDaylightSavingTransitions() {
        assertThat(DateUtils.format(Instant.parse("2024-03-31T00:30:00Z").toEpochMilli(), ZoneId.of("Europe/Kiev")))
                .isEqualTo("2024-03-31T02:30:00+02:00");
        assertThat(DateUtils.format(Instant.parse("2024-03-31T01:30:00Z").toEpochMilli(), ZoneId.of("Europe/Kiev")))
                .isEqualTo("2024-03-31T04:30:00+03:00");
        assertThat(DateUtils.format(null, DateUtils.UTC)).isNull();
    }
}
