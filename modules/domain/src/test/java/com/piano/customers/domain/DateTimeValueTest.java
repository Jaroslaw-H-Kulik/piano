package com.piano.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DateTimeValueTest {

    @Test
    void parsesTheFormatUsedInTheTaskExample() {
        DateTimeValue value = DateTimeValue.parse("1990-01-01T00:00:00");

        assertThat(value.value()).isEqualTo(LocalDateTime.of(1990, 1, 1, 0, 0, 0));
    }

    @Test
    void writesMidnightWithSecondsSoThatTheTextRoundTrips() {
        DateTimeValue value = new DateTimeValue(LocalDateTime.of(1990, 1, 1, 0, 0));

        assertThat(value.rawValue()).isEqualTo("1990-01-01T00:00:00");
    }

    @Test
    void textRoundTripsUnchanged() {
        String text = "2024-02-29T23:59:58";

        assertThat(DateTimeValue.parse(text).rawValue()).isEqualTo(text);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1990-01-01T00:00:00Z",
                "1990-01-01T02:00:00+02:00",
                "1990-01-01T00:00:00.123",
                "1990-01-01T00:00:00.123456789-05:00"
            })
    void offsetsAndFractionsRoundTripUnchanged(String text) {
        assertThat(DateTimeValue.parse(text).rawValue()).isEqualTo(text);
    }

    @Test
    void keepsTheOffsetAsReceived() {
        DateTimeValue value = DateTimeValue.parse("1990-01-01T02:00:00+02:00");

        assertThat(value.value()).isEqualTo(LocalDateTime.of(1990, 1, 1, 2, 0, 0));
        assertThat(value.offset()).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    void acceptsMissingSeconds() {
        assertThat(DateTimeValue.parse("1990-01-01T00:00").rawValue())
                .isEqualTo("1990-01-01T00:00:00");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1990-01-01",
                "1990-01-01 00:00:00",
                "1990-02-30T00:00:00",
                "1990-13-01T00:00:00",
                "not a date",
                ""
            })
    void rejectsTextThatIsNotAnIsoDateTime(String text) {
        assertThatThrownBy(() -> DateTimeValue.parse(text))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void hasTheDatetimeType() {
        assertThat(DateTimeValue.parse("1990-01-01T00:00:00").type()).isEqualTo(FieldType.DATETIME);
    }
}
