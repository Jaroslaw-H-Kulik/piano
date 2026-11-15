package com.piano.customers.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.piano.customers.domain.BooleanValue;
import com.piano.customers.domain.DateTimeValue;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.FieldValue;
import com.piano.customers.domain.IntegerValue;
import com.piano.customers.domain.StringValue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GenericFieldsJsonTest {

    @Test
    void writesEachTypeAsItsNaturalJsonValue() {
        Map<FieldName, FieldValue> fields = new LinkedHashMap<>();
        fields.put(new FieldName("vip"), new BooleanValue(true));
        fields.put(new FieldName("score"), new IntegerValue(100));
        fields.put(new FieldName("nickname"), new StringValue("Johnny"));
        fields.put(new FieldName("dob"), DateTimeValue.parse("1990-01-01T00:00:00"));

        String text = GenericFieldsJson.toJson(fields);

        Map<FieldName, Object> raw = GenericFieldsJson.fromJson(text);
        assertThat(raw)
                .containsOnly(
                        Map.entry(new FieldName("vip"), true),
                        Map.entry(new FieldName("score"), 100L),
                        Map.entry(new FieldName("nickname"), "Johnny"),
                        Map.entry(new FieldName("dob"), "1990-01-01T00:00:00"));
    }

    @Test
    void writesNumbersAndBooleansWithoutQuotes() {
        Map<FieldName, FieldValue> fields = Map.of(new FieldName("score"), new IntegerValue(100));

        assertThat(GenericFieldsJson.toJson(fields)).isEqualTo("{\"score\":100}");
    }

    @Test
    void writesNoFieldsAsAnEmptyObject() {
        assertThat(GenericFieldsJson.toJson(Map.of())).isEqualTo("{}");
    }

    @Test
    void readsAnEmptyObjectAsNoFields() {
        assertThat(GenericFieldsJson.fromJson("{}")).isEmpty();
    }

    @Test
    void readsLargeIntegersWithoutLosingPrecision() {
        Map<FieldName, Object> raw = GenericFieldsJson.fromJson("{\"big\":9223372036854775807}");

        assertThat(raw).containsEntry(new FieldName("big"), Long.MAX_VALUE);
    }

    @Test
    void refusesStoredValuesOfUnsupportedJsonTypes() {
        assertThatThrownBy(() -> GenericFieldsJson.fromJson("{\"weird\":[1,2]}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored generic field weird has an unsupported JSON value");
    }
}
