package com.example.baget.orders;

import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerRepository;
import com.example.baget.util.NotFoundException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;

    public OrdersService(final OrdersRepository ordersRepository, CustomerRepository customerRepository) {
        this.ordersRepository = ordersRepository;
        this.customerRepository = customerRepository;
    }

    public List<OrdersDTO> findAll() {
        final List<Orders> orderses = ordersRepository.findAll(Sort.by("orderNo"));
        return orderses.stream()
                .map(orders -> mapToDTO(orders, new OrdersDTO()))
                .toList();
    }

    public Page<OrdersDTO> findAll(Pageable pageable) {
        Page<Orders> ordersPage = ordersRepository.findAll(pageable);
        System.out.println("Has Previous: " + ordersPage.hasPrevious());
        System.out.println("Current Page: " + ordersPage.getNumber());
        System.out.println("Size: " + ordersPage.getSize());
        System.out.println("Total Pages: " + ordersPage.getTotalPages());
        System.out.println("Has Next: " + ordersPage.hasNext());
        return ordersPage.map(orders -> mapToDTO(orders, new OrdersDTO()));
    }

    public OrdersDTO get(final Long orderNo) {
        return ordersRepository.findById(orderNo)
                .map(orders -> mapToDTO(orders, new OrdersDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final OrdersDTO ordersDTO) {
        final Orders orders = new Orders();
        mapToEntity(ordersDTO, orders);
        return ordersRepository.save(orders).getOrderNo();
    }

    public void update(final Long orderNo, final OrdersDTO ordersDTO) {
        final Orders orders = ordersRepository.findById(orderNo)
                .orElseThrow(NotFoundException::new);
        mapToEntity(ordersDTO, orders);
        ordersRepository.save(orders);
    }

    public void delete(final Long orderNo) {
        ordersRepository.deleteById(orderNo);
    }

    private OrdersDTO mapToDTO(final Orders orders, final OrdersDTO ordersDTO) {
        ordersDTO.setOrderNo(orders.getOrderNo());
        ordersDTO.setCustNo(orders.getCustomer().getCustNo());
        ordersDTO.setFactNo(orders.getFactNo());
        ordersDTO.setSaleDate(orders.getSaleDate());
        ordersDTO.setShipDate(orders.getShipDate());
        ordersDTO.setEmpNo(orders.getEmpNo());
        ordersDTO.setShipToContact(orders.getShipToContact());
        ordersDTO.setShipToAddr1(orders.getShipToAddr1());
        ordersDTO.setShipToAddr2(orders.getShipToAddr2());
        ordersDTO.setShipToCity(orders.getShipToCity());
        ordersDTO.setShipToState(orders.getShipToState());
        ordersDTO.setShipToZip(orders.getShipToZip());
        ordersDTO.setShipToCountry(orders.getShipToCountry());
        ordersDTO.setShipToPhone(orders.getShipToPhone());
        ordersDTO.setShipVia(orders.getShipVia());
        ordersDTO.setPo(orders.getPo());
        ordersDTO.setTerms(orders.getTerms());
        ordersDTO.setPaymentMethod(orders.getPaymentMethod());
        ordersDTO.setItemsTotal(orders.getItemsTotal());
        ordersDTO.setTaxRate(orders.getTaxRate());
        ordersDTO.setFreight(orders.getFreight());
        ordersDTO.setAmountPaid(orders.getAmountPaid());
        ordersDTO.setAmountDueN(orders.getAmountDueN());
        ordersDTO.setIncome(orders.getIncome());
        ordersDTO.setPriceLevel(orders.getPriceLevel());
        ordersDTO.setStatusOrder(orders.getStatusOrder());
        ordersDTO.setRahFacNo(orders.getRahFacNo());
        return ordersDTO;
    }

    private Orders mapToEntity(final OrdersDTO ordersDTO, final Orders orders) {
        Customer customer = customerRepository.findById(ordersDTO.getCustNo())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        orders.setCustomer(customer);
        orders.setFactNo(ordersDTO.getFactNo());
        orders.setSaleDate(ordersDTO.getSaleDate());
        orders.setShipDate(ordersDTO.getShipDate());
        orders.setEmpNo(ordersDTO.getEmpNo());
        orders.setShipToContact(ordersDTO.getShipToContact());
        orders.setShipToAddr1(ordersDTO.getShipToAddr1());
        orders.setShipToAddr2(ordersDTO.getShipToAddr2());
        orders.setShipToCity(ordersDTO.getShipToCity());
        orders.setShipToState(ordersDTO.getShipToState());
        orders.setShipToZip(ordersDTO.getShipToZip());
        orders.setShipToCountry(ordersDTO.getShipToCountry());
        orders.setShipToPhone(ordersDTO.getShipToPhone());
        orders.setShipVia(ordersDTO.getShipVia());
        orders.setPo(ordersDTO.getPo());
        orders.setTerms(ordersDTO.getTerms());
        orders.setPaymentMethod(ordersDTO.getPaymentMethod());
        orders.setItemsTotal(ordersDTO.getItemsTotal());
        orders.setTaxRate(ordersDTO.getTaxRate());
        orders.setFreight(ordersDTO.getFreight());
        orders.setAmountPaid(ordersDTO.getAmountPaid());
        orders.setAmountDueN(ordersDTO.getAmountDueN());
        orders.setIncome(ordersDTO.getIncome());
        orders.setPriceLevel(ordersDTO.getPriceLevel());
        orders.setStatusOrder(ordersDTO.getStatusOrder());
        orders.setRahFacNo(ordersDTO.getRahFacNo());
        return orders;
    }

}
