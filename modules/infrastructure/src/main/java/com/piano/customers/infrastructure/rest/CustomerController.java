package com.piano.customers.infrastructure.rest;

import com.piano.customers.application.CreateCustomer;
import com.piano.customers.application.CreateCustomerCommand;
import com.piano.customers.application.DeleteCustomer;
import com.piano.customers.application.GetCustomer;
import com.piano.customers.application.UpdateCustomer;
import com.piano.customers.application.UpdateCustomerCommand;
import com.piano.customers.domain.Customer;
import com.piano.customers.domain.CustomerId;
import com.piano.customers.domain.TenantId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer CRUD. The {@link TenantId} parameters are read from the tenant header ({@link
 * TenantIdArgumentResolver}).
 */
@RestController
public class CustomerController {

    private final CreateCustomer createCustomer;
    private final GetCustomer getCustomer;
    private final UpdateCustomer updateCustomer;
    private final DeleteCustomer deleteCustomer;

    public CustomerController(
            CreateCustomer createCustomer,
            GetCustomer getCustomer,
            UpdateCustomer updateCustomer,
            DeleteCustomer deleteCustomer) {
        this.createCustomer = createCustomer;
        this.getCustomer = getCustomer;
        this.updateCustomer = updateCustomer;
        this.deleteCustomer = deleteCustomer;
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.OK)
    public CustomerResponse create(TenantId tenantId, @RequestBody CreateCustomerRequest request) {
        CreateCustomerCommand command = request.toCommand(tenantId);

        Customer created = createCustomer.execute(command);

        return CustomerResponse.from(created);
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse get(TenantId tenantId, @PathVariable String id) {
        Customer customer = getCustomer.execute(tenantId, new CustomerId(id));

        return CustomerResponse.from(customer);
    }

    @PatchMapping("/customers/{id}")
    public CustomerResponse update(
            TenantId tenantId,
            @PathVariable String id,
            @RequestBody UpdateCustomerRequest request) {
        UpdateCustomerCommand command = request.toCommand(tenantId, new CustomerId(id));

        Customer updated = updateCustomer.execute(command);

        return CustomerResponse.from(updated);
    }

    @DeleteMapping("/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(TenantId tenantId, @PathVariable String id) {
        deleteCustomer.execute(tenantId, new CustomerId(id));
    }
}
