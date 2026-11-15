package com.piano.customers.domain;

import java.util.Optional;

public interface CustomerRepository {

    /**
     * @throws DuplicateCustomerException if the tenant already has a customer with this id
     */
    void insert(Customer customer);

    Optional<Customer> findById(TenantId tenantId, CustomerId id);

    /**
     * Saves a customer that was read earlier, using its version for optimistic locking.
     *
     * @return the saved customer with its new version
     * @throws CustomerModifiedConcurrentlyException if no customer with this id and version exists
     *     any more, because it was changed or deleted since it was read (the repository does not
     *     tell the two apart)
     */
    Customer update(Customer customer);

    /**
     * @return true if a customer was deleted, false if there was none
     */
    boolean delete(TenantId tenantId, CustomerId id);
}
