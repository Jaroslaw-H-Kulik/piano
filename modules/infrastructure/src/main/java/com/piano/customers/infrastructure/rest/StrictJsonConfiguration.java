package com.piano.customers.infrastructure.rest;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration
class StrictJsonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer rejectNonStringValuesForStringProperties() {
        return builder ->
                builder.withCoercionConfig(
                        LogicalType.Textual,
                        config ->
                                config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                        .setCoercion(
                                                CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
