package com.piano.customers.application;

import com.piano.customers.domain.Customer;

public interface UpdateCustomer {

    Customer execute(UpdateCustomerCommand command);
}
