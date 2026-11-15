package com.piano.customers.application;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.CustomerNotFoundException;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.TenantId;

public class GetCustomerService implements GetCustomer {

    private final CustomerRepository customerRepository;

    public GetCustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer execute(TenantId tenantId, CustomerId id) {
        return customerRepository
                .findById(tenantId, id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
