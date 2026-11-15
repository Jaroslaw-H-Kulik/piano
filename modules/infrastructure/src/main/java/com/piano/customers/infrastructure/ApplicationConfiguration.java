package com.piano.customers.infrastructure;

import com.piano.customers.application.CreateCustomer;
import com.piano.customers.application.CreateCustomerService;
import com.piano.customers.application.DeleteCustomer;
import com.piano.customers.application.DeleteCustomerService;
import com.piano.customers.application.GetCustomer;
import com.piano.customers.application.GetCustomerService;
import com.piano.customers.application.RegisterGenericField;
import com.piano.customers.application.RegisterGenericFieldService;
import com.piano.customers.application.UpdateCustomer;
import com.piano.customers.application.UpdateCustomerService;
import com.piano.customers.domain.CustomerRepository;
import com.piano.customers.domain.GenericFieldDefinitionRepository;
import com.piano.customers.infrastructure.persistence.JdbcCustomerRepository;
import com.piano.customers.infrastructure.persistence.JdbcGenericFieldDefinitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
public class ApplicationConfiguration {

    @Bean
    GenericFieldDefinitionRepository genericFieldDefinitionRepository(JdbcClient jdbc) {
        return new JdbcGenericFieldDefinitionRepository(jdbc);
    }

    @Bean
    CustomerRepository customerRepository(
            JdbcClient jdbc, GenericFieldDefinitionRepository definitionRepository) {
        return new JdbcCustomerRepository(jdbc, definitionRepository);
    }

    @Bean
    RegisterGenericField registerGenericField(
            GenericFieldDefinitionRepository definitionRepository) {
        return new RegisterGenericFieldService(definitionRepository);
    }

    @Bean
    CreateCustomer createCustomer(
            CustomerRepository customerRepository,
            GenericFieldDefinitionRepository definitionRepository) {
        return new CreateCustomerService(customerRepository, definitionRepository);
    }

    @Bean
    GetCustomer getCustomer(CustomerRepository customerRepository) {
        return new GetCustomerService(customerRepository);
    }

    @Bean
    UpdateCustomer updateCustomer(
            CustomerRepository customerRepository,
            GenericFieldDefinitionRepository definitionRepository) {
        return new UpdateCustomerService(customerRepository, definitionRepository);
    }

    @Bean
    DeleteCustomer deleteCustomer(CustomerRepository customerRepository) {
        return new DeleteCustomerService(customerRepository);
    }
}
