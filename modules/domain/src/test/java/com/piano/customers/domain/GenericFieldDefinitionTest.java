package com.piano.customers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenericFieldDefinitionTest {

    private static final TenantId TENANT = new TenantId("tenant_123");

    private static GenericFieldDefinition definitionOf(String name, FieldType type) {
        return new GenericFieldDefinition(TENANT, new FieldName(name), type);
    }

    @Test
    void parsingTheRawValueOfEachTypeGivesBackAnEqualValue() {
        List<FieldValue> values =
                List.of(
                        new BooleanValue(true),
                        new IntegerValue(100),
                        new StringValue("Johnny"),
                        new DateTimeValue(LocalDateTime.of(1990, 1, 1, 0, 0)));

        for (FieldValue value : values) {
            GenericFieldDefinition definition = definitionOf("field", value.type());

            assertThat(definition.parse(value.rawValue())).isEqualTo(value);
        }
    }

    @Test
    void parsesBooleanForBoolType() {
        FieldValue value = definitionOf("vip", FieldType.BOOL).parse(true);

        assertThat(value).isEqualTo(new BooleanValue(true));
    }

    @Test
    void parsesLongForIntegerType() {
        FieldValue value = definitionOf("score", FieldType.INTEGER).parse(100L);

        assertThat(value).isEqualTo(new IntegerValue(100));
    }

    @Test
    void parsesStringForStringType() {
        FieldValue value = definitionOf("nickname", FieldType.STRING).parse("Johnny");

        assertThat(value).isEqualTo(new StringValue("Johnny"));
    }

    @Test
    void parsesIsoTextForDatetimeType() {
        FieldValue value = definitionOf("dob", FieldType.DATETIME).parse("1990-01-01T00:00:00");

        assertThat(value).isEqualTo(new DateTimeValue(LocalDateTime.of(1990, 1, 1, 0, 0, 0)));
    }

    @Test
    void doesNotConvertBetweenTypes() {
        GenericFieldDefinition integerField = definitionOf("score", FieldType.INTEGER);

        assertThatThrownBy(() -> integerField.parse("100"))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field score expects INTEGER but received a string");
    }

    @Test
    void rejectsIntegerWhereBooleanIsExpected() {
        GenericFieldDefinition booleanField = definitionOf("vip", FieldType.BOOL);

        assertThatThrownBy(() -> booleanField.parse(1L))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field vip expects BOOL but received an integer");
    }

    @Test
    void rejectsBooleanWhereStringIsExpected() {
        GenericFieldDefinition stringField = definitionOf("nickname", FieldType.STRING);

        assertThatThrownBy(() -> stringField.parse(true))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field nickname expects STRING but received a boolean");
    }

    @Test
    void rejectsNonTextWhereDatetimeIsExpected() {
        GenericFieldDefinition datetimeField = definitionOf("dob", FieldType.DATETIME);

        assertThatThrownBy(() -> datetimeField.parse(19900101L))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field dob expects DATETIME but received an integer");
    }

    @Test
    void acceptsDatetimeTextWithOffset() {
        GenericFieldDefinition datetimeField = definitionOf("dob", FieldType.DATETIME);

        assertThat(datetimeField.parse("1990-01-01T00:00:00Z").rawValue())
                .isEqualTo("1990-01-01T00:00:00Z");
    }

    @Test
    void rejectsTextThatIsNotADatetime() {
        GenericFieldDefinition datetimeField = definitionOf("dob", FieldType.DATETIME);

        assertThatThrownBy(() -> datetimeField.parse("1990-01-01"))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage(
                        "Field dob expects a DATETIME in ISO 8601, e.g. 1990-01-01T00:00:00 or 1990-01-01T00:00:00Z");
    }

    @Test
    void rejectsNullBecauseClearingIsExpressedAsAChange() {
        GenericFieldDefinition integerField = definitionOf("score", FieldType.INTEGER);

        assertThatThrownBy(() -> integerField.parse(null))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field score expects INTEGER but received null");
    }

    @Test
    void rejectsValuesOfUnsupportedJavaTypes() {
        GenericFieldDefinition integerField = definitionOf("score", FieldType.INTEGER);

        assertThatThrownBy(() -> integerField.parse(1.5))
                .isInstanceOf(InvalidFieldValueException.class)
                .hasMessage("Field score expects INTEGER but received an unsupported value");
    }
}
