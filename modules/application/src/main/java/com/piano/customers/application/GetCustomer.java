package com.piano.customers.application;

import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.TenantId;

public interface GetCustomer {

    Customer execute(TenantId tenantId, CustomerId id);
}
