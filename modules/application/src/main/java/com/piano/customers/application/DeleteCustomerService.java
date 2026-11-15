package com.piano.customers.application;

import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.CustomerNotFoundException;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.TenantId;

public class DeleteCustomerService implements DeleteCustomer {

    private final CustomerRepository customerRepository;

    public DeleteCustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void execute(TenantId tenantId, CustomerId id) {
        boolean deleted = customerRepository.delete(tenantId, id);
        if (!deleted) {
            throw new CustomerNotFoundException(id);
        }
    }
}
