package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.invoices.InvoiceRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UsersRepository usersRepository;
    private final InvoiceRepository invoiceRepository;

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

//    @CacheEvict(value = "CustomerPhonePrefix", allEntries = true)
    public Long create(final CustomerDTO customerDTO) {
        final Customer customer = new Customer();
        mapToEntity(customerDTO, customer);
        return customerRepository.save(customer).getCustNo();
    }

    @CacheEvict(value = "CustomerPhonePrefix", key = "#customerDTO.phone")
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
        customerDTO.setMobile(customer.getMobile());
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
        customer.setMobile(customerDTO.getMobile());
        customer.setTaxRate(customerDTO.getTaxRate());
        customer.setContact(customerDTO.getContact());
        customer.setLastInvoiceDate(customerDTO.getLastInvoiceDate());
        customer.setPriceLevel(customerDTO.getPriceLevel());
    }

    @Cacheable(value = "CustomerPhonePrefix", key = "#prefix")
    public List<CustomerDTO> findByPhonePrefix(String prefix) {
        return customerRepository.findByMobileContaining(prefix)
                .stream()
                .map(customer -> new CustomerDTO(customer.getCustNo(), customer.getCompany(), customer.getAddr1(), customer.getMobile(), customer.getComment()))
                .collect(Collectors.toList());

    }

    public List<CustomerBalanceDTO> getClientsForManager(String username, Long branchNo, boolean debtOnly, LocalDate date) {
        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        Set<Long> allowedBranchNos = user.getAllowedBranches()
                .stream()
                .map(Branch::getBranchNo)
                .collect(Collectors.toSet());

        if (branchNo != null && !allowedBranchNos.contains(branchNo)) {
            throw new AccessDeniedException("Branch not allowed");
        }

        Collection<Long> branchesToUse =
                (branchNo != null) ? List.of(branchNo) : allowedBranchNos;

        OffsetDateTime dateEnd = (date != null) ? date.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC) : null;

        return customerRepository.findClientBalances(branchesToUse, debtOnly, dateEnd);
    }


    public List<CustomerInvoiceDTO> getInvoicesByCustomer(Long custNo) {
        return invoiceRepository.findInvoicesByCustomer(custNo);
    }

    public List<CustomerSelectDTO> getCustomersForInvoice() {
        return customerRepository.findAllBy().stream()
                .map(c -> new CustomerSelectDTO(c.getCustNo(), c.getCompany()))
                .toList();
    }
}
