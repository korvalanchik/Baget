package com.example.baget.orders;

import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerRepository;
import com.example.baget.items.*;
import com.example.baget.util.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsService itemsService;
    public OrdersService(final OrdersRepository ordersRepository, CustomerRepository customerRepository, ItemsRepository itemsRepository, ItemsService itemsService) {
        this.ordersRepository = ordersRepository;
        this.customerRepository = customerRepository;
        this.itemsRepository = itemsRepository;
        this.itemsService = itemsService;
    }

    public List<OrdersDTO> findAll() {
        final List<Orders> orders = ordersRepository.findAll(Sort.by("orderNo"));
        return orders.stream()
                .map(order -> mapToDTO(order, new OrdersDTO()))
                .toList();
    }

    public Page<OrdersDTO> findAll(Pageable pageable) {
        Page<Orders> ordersPage = ordersRepository.findAll(pageable);
        return ordersPage.map(orders -> mapToDTO(orders, new OrdersDTO()));
    }

    public OrdersDTO get(final Long orderNo) {
        return ordersRepository.findById(orderNo)
                .map(orders -> mapToDTO(orders, new OrdersDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Page<OrdersDTO> getOrders(Pageable pageable) {
        // Виклик репозиторію для отримання сторінки замовлень
        return ordersRepository.findAll(pageable)
                .map(orders -> mapToDTO(orders, new OrdersDTO())); // Перетворення у DTO, якщо потрібно
    }

    @Transactional
    public Long create(final OrdersDTO ordersDTO) {
        final Orders orders = new Orders();
        mapToEntity(ordersDTO, orders);
        ordersRepository.save(orders);
        saveItems(orders, ordersDTO);
        return orders.getOrderNo();
    }

    @Transactional
    public void update(final Long orderNo, final OrdersDTO ordersDTO) {
        final Orders existingOrder = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        mapToEntity(ordersDTO, existingOrder);
//        Тимчасово відключили оновлення Items
        updateItems(existingOrder, ordersDTO);
        ordersRepository.save(existingOrder);
    }

    public void delete(final Long orderNo) {
        ordersRepository.deleteById(orderNo);
    }

    private OrdersDTO mapToDTO(final Orders orders, final OrdersDTO ordersDTO) {
        ordersDTO.setOrderNo(orders.getOrderNo());
        ordersDTO.setCustNo(orders.getCustomer().getCustNo());
        ordersDTO.setCompany(orders.getCustomer().getCompany());
        ordersDTO.setPhone(orders.getCustomer().getPhone());
        ordersDTO.setItems(orders.getItems().stream()
                .map(item -> itemsService.mapToDTO(item, new ItemsDTO()))
                .collect(Collectors.toList()));
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

    private void mapToEntity(final OrdersDTO ordersDTO, final Orders orders) {
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
    }

    private void saveItems(final Orders orders, final OrdersDTO ordersDTO) {
        if(ordersDTO.getItems() != null) {
            List<Items> items = new ArrayList<>();
            Long itemNo = 1L;
            for (ItemsDTO itemDTO : ordersDTO.getItems()) {
                Items item = new Items();
                ItemId itemId = new ItemId(orders.getOrderNo(), itemNo);
                item.setId(itemId);
                item.setOrder(orders);
                item.setPartNo(itemDTO.getPartNo());
                item.setProfilWidth(itemDTO.getProfilWidth());
                item.setWidth(itemDTO.getWidth());
                item.setHeight(itemDTO.getHeight());
                item.setQty(itemDTO.getQty());
                item.setQuantity(itemDTO.getQuantity());
                item.setSellPrice(itemDTO.getSellPrice());
                item.setDiscount(itemDTO.getDiscount());
                item.setOnHand(itemDTO.getOnHand());
                item.setCost(itemDTO.getCost());

                itemsRepository.save(item);
                itemNo++;
            }
        }
    }

    private void updateItems(final Orders existingOrder, final OrdersDTO ordersDTO) {
        deleteAllItemsByOrderNo(existingOrder.getOrderNo());
        saveItems(existingOrder, ordersDTO);
    }

    public void deleteAllItemsByOrderNo(Long orderNo) {
        itemsRepository.deleteByOrderOrderNo(orderNo);
    }

}
