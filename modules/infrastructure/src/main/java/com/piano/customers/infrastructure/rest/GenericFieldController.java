package com.piano.customers.infrastructure.rest;

import com.piano.customers.application.RegisterGenericField;
import com.piano.customers.application.RegisterGenericFieldCommand;
import com.piano.customers.domain.GenericFieldDefinition;
import com.piano.customers.domain.TenantId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Generic field registration. The {@link TenantId} parameter is read from the tenant header. */
@RestController
public class GenericFieldController {

    private final RegisterGenericField registerGenericField;

    public GenericFieldController(RegisterGenericField registerGenericField) {
        this.registerGenericField = registerGenericField;
    }

    @PostMapping("/generic_fields")
    @ResponseStatus(HttpStatus.OK)
    public GenericFieldResponse register(
            TenantId tenantId, @RequestBody GenericFieldRequest request) {
        RegisterGenericFieldCommand command = request.toCommand(tenantId);

        GenericFieldDefinition registered = registerGenericField.execute(command);

        return GenericFieldResponse.from(registered);
    }
}
