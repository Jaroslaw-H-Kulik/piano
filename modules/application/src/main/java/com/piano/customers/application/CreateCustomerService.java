package com.piano.customers.application;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.GenericFieldDefinitionRepository;
import java.util.Map;

public class CreateCustomerService implements CreateCustomer {

    private final CustomerRepository customerRepository;
    private final GenericFieldDefinitionRepository definitionRepository;

    public CreateCustomerService(
            CustomerRepository customerRepository,
            GenericFieldDefinitionRepository definitionRepository) {
        this.customerRepository = customerRepository;
        this.definitionRepository = definitionRepository;
    }

    @Override
    public Customer execute(CreateCustomerCommand command) {
        Map<FieldName, GenericFieldDefinition> fieldDefinitions =
                definitionRepository.findByNames(
                        command.tenantId(), command.genericFields().keySet());

        Customer customer =
                Customer.create(
                        command.tenantId(),
                        command.id(),
                        command.firstName(),
                        command.lastName(),
                        command.genericFields(),
                        fieldDefinitions);

        customerRepository.insert(customer);
        return customer;
    }
}
