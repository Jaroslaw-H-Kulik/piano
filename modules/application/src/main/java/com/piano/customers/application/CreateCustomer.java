package com.piano.customers.application;

import com.piano.customers.domain.Customer;

public interface CreateCustomer {

    Customer execute(CreateCustomerCommand command);
}
