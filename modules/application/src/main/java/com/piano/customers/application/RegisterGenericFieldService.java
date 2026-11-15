package com.piano.customers.application;

import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.GenericFieldDefinitionRepository;

public class RegisterGenericFieldService implements RegisterGenericField {

    private final GenericFieldDefinitionRepository definitionRepository;

    public RegisterGenericFieldService(GenericFieldDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    @Override
    public GenericFieldDefinition execute(RegisterGenericFieldCommand command) {
        GenericFieldDefinition definition =
                new GenericFieldDefinition(command.tenantId(), command.name(), command.type());
        definitionRepository.insert(definition);
        return definition;
    }
}
