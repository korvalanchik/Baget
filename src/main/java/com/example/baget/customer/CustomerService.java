package com.example.baget.customer;

import com.example.baget.util.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(final CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerDTO> findAll() {
        final List<Customer> customers = customerRepository.findAll(Sort.by("custNo"));
        return customers.stream()
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomers(Pageable pageable) {
        // Виклик репозиторію для отримання сторінки замовлень
        return customerRepository.findAll(pageable)
                .map(customers -> mapToDTO(customers, new CustomerDTO())); // Перетворення у DTO, якщо потрібно
    }

    public CustomerDTO get(final Long custNo) {
        return customerRepository.findById(custNo)
                .map(customer -> mapToDTO(customer, new CustomerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final CustomerDTO customerDTO) {
        final Customer customer = new Customer();
        mapToEntity(customerDTO, customer);
        return customerRepository.save(customer).getCustNo();
    }

    public void update(final Long custNo, final CustomerDTO customerDTO) {
        final Customer customer = customerRepository.findById(custNo)
                .orElseThrow(NotFoundException::new);
        mapToEntity(customerDTO, customer);
        customerRepository.save(customer);
    }

    public void delete(final Long custNo) {
        customerRepository.deleteById(custNo);
    }

    private CustomerDTO mapToDTO(final Customer customer, final CustomerDTO customerDTO) {
        customerDTO.setCustNo(customer.getCustNo());
        customerDTO.setCompany(customer.getCompany());
        customerDTO.setAddr1(customer.getAddr1());
        customerDTO.setComment(customer.getComment());
        customerDTO.setCity(customer.getCity());
        customerDTO.setState(customer.getState());
        customerDTO.setZip(customer.getZip());
        customerDTO.setCountry(customer.getCountry());
        customerDTO.setPhone(customer.getPhone());
        customerDTO.setFax(customer.getFax());
        customerDTO.setTaxRate(customer.getTaxRate());
        customerDTO.setContact(customer.getContact());
        customerDTO.setLastInvoiceDate(customer.getLastInvoiceDate());
        customerDTO.setPriceLevel(customer.getPriceLevel());
        return customerDTO;
    }

    private void mapToEntity(final CustomerDTO customerDTO, final Customer customer) {
        customer.setCompany(customerDTO.getCompany());
        customer.setAddr1(customerDTO.getAddr1());
        customer.setComment(customerDTO.getComment());
        customer.setCity(customerDTO.getCity());
        customer.setState(customerDTO.getState());
        customer.setZip(customerDTO.getZip());
        customer.setCountry(customerDTO.getCountry());
        customer.setPhone(customerDTO.getPhone());
        customer.setFax(customerDTO.getFax());
        customer.setTaxRate(customerDTO.getTaxRate());
        customer.setContact(customerDTO.getContact());
        customer.setLastInvoiceDate(customerDTO.getLastInvoiceDate());
        customer.setPriceLevel(customerDTO.getPriceLevel());
    }

    public List<CustomerDTO> findByPhonePrefix(String prefix) {
        return customerRepository.findByPhoneContaining(prefix)
                .stream()
                .map(customer -> new CustomerDTO(customer.getCustNo(), customer.getCompany(), customer.getPhone()))
                .collect(Collectors.toList());

    }
}
