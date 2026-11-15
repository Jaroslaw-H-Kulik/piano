package com.piano.customers.application;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerModifiedConcurrentlyException;
import com.piano.customers.domain.CustomerNotFoundException;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.FieldName;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.GenericFieldDefinitionRepository;
import java.util.Map;

public class UpdateCustomerService implements UpdateCustomer {

    /**
     * How often an update is tried when another request keeps changing the same customer. A patch
     * only touches the attributes it names, so re-applying it to the newer state is safe; the limit
     * just bounds the work.
     */
    private static final int MAX_ATTEMPTS = 3;

    private final CustomerRepository customerRepository;
    private final GenericFieldDefinitionRepository definitionRepository;

    public UpdateCustomerService(
            CustomerRepository customerRepository,
            GenericFieldDefinitionRepository definitionRepository) {
        this.customerRepository = customerRepository;
        this.definitionRepository = definitionRepository;
    }

    @Override
    public Customer execute(UpdateCustomerCommand command) {
        for (int attempt = 1; ; attempt++) {
            try {
                return readApplyAndSave(command);
            } catch (CustomerModifiedConcurrentlyException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                // Read again. If the customer was deleted meanwhile, that read ends in
                // CustomerNotFoundException.
            }
        }
    }

    private Customer readApplyAndSave(UpdateCustomerCommand command) {
        Customer stored =
                customerRepository
                        .findById(command.tenantId(), command.id())
                        .orElseThrow(() -> new CustomerNotFoundException(command.id()));

        Map<FieldName, GenericFieldDefinition> fieldDefinitions =
                definitionRepository.findByNames(
                        command.tenantId(), command.changes().genericFieldNames());

        Customer changed = stored.applyChanges(command.changes(), fieldDefinitions);

        return customerRepository.update(changed);
    }
}
