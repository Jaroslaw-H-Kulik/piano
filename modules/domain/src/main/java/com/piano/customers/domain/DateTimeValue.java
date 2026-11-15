package com.piano.customers.domain;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * An ISO 8601 date-time, with or without a UTC offset ({@code 1990-01-01T00:00:00}, {@code
 * 1990-01-01T00:00:00Z}, {@code 1990-01-01T02:00:00+02:00}). The offset is kept as received, so a
 * fetched value reads the same as the one sent; a value without an offset stays without one.
 *
 * @param value the local date and time, as written
 * @param offset the offset, or {@code null} if the text had none
 */
public record DateTimeValue(LocalDateTime value, @Nullable ZoneOffset offset)
        implements FieldValue {

    public static final String FORMAT_DESCRIPTION =
            "ISO 8601, e.g. 1990-01-01T00:00:00 or 1990-01-01T00:00:00Z";

    /**
     * ISO 8601 with seconds, fraction and offset optional. Unlike {@link
     * DateTimeFormatter#ISO_DATE_TIME} it rejects a zone name ({@code [Europe/Paris]}), which could
     * not be stored and so would not round-trip. Strict resolving keeps {@code 1990-02-30} invalid.
     */
    private static final DateTimeFormatter PARSER =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .optionalStart()
                    .appendOffsetId()
                    .optionalEnd()
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT)
                    .withChronology(IsoChronology.INSTANCE);

    /** Always writes seconds, so midnight is {@code T00:00:00} and not {@code T00:00}. */
    private static final DateTimeFormatter LOCAL_WRITER =
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral('T')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .toFormatter();

    public DateTimeValue {
        Objects.requireNonNull(value, "value");
    }

    public DateTimeValue(LocalDateTime value) {
        this(value, null);
    }

    /**
     * @throws java.time.format.DateTimeParseException if the text is not an ISO 8601 date-time
     */
    public static DateTimeValue parse(String text) {
        Object parsed = PARSER.parseBest(text, OffsetDateTime::from, LocalDateTime::from);
        if (parsed instanceof OffsetDateTime offsetDateTime) {
            return new DateTimeValue(offsetDateTime.toLocalDateTime(), offsetDateTime.getOffset());
        }
        return new DateTimeValue((LocalDateTime) parsed, null);
    }

    @Override
    public FieldType type() {
        return FieldType.DATETIME;
    }

    /** The value as ISO 8601 text, which {@link #parse} reads back to an equal value. */
    @Override
    public String rawValue() {
        String local = LOCAL_WRITER.format(value);
        if (offset == null) {
            return local;
        }
        return local + offset.getId();
    }
}
