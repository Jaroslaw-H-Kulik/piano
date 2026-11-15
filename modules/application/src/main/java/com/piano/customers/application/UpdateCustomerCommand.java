package com.piano.customers.application;

import com.piano.customers.domain.CustomerChanges;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.TenantId;

public record UpdateCustomerCommand(TenantId tenantId, CustomerId id, CustomerChanges changes) {}
