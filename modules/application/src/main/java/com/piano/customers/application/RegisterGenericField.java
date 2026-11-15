package com.piano.customers.application;

import com.piano.customers.domain.GenericFieldDefinition;

public interface RegisterGenericField {

    GenericFieldDefinition execute(RegisterGenericFieldCommand command);
}
