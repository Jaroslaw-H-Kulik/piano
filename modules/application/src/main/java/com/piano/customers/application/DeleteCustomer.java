package com.piano.customers.application;

import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.TenantId;

public interface DeleteCustomer {

    void execute(TenantId tenantId, CustomerId id);
}
