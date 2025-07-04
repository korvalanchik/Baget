package com.example.baget.orders;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerDTO;
import com.example.baget.customer.CustomerRepository;
import com.example.baget.customer.CustomerService;
import com.example.baget.items.*;
import com.example.baget.users.Role;
import com.example.baget.users.User;
import com.example.baget.users.UserCacheService;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final UsersRepository userRepository;
    private final UserCacheService userCacheService;
    private final BranchRepository branchRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsService itemsService;
    public OrdersService(final OrdersRepository ordersRepository, CustomerRepository customerRepository,
                         CustomerService customerService, UsersRepository userRepository,
                         UserCacheService userCacheService, BranchRepository branchRepository,
                         ItemsRepository itemsRepository, ItemsService itemsService) {
        this.ordersRepository = ordersRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.userRepository = userRepository;
        this.userCacheService = userCacheService;
        this.branchRepository = branchRepository;
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

//    public Page<OrdersDTO> getOrders(Pageable pageable, String requestedBranchName) {
//
//        System.out.println("Pageable: " + pageable);
//        System.out.println("Сортування: " + pageable.getSort());
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String username = auth.getName();
//
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        Set<String> userRoles = user.getRoles().stream()
//                .map(Role::getName)
//                .collect(Collectors.toSet());
//
//        Map<Long, String> userIdUsernameMap = userCacheService.loadMap();
//
//        // ADMIN бачить усе
//        if (userRoles.contains("ROLE_ADMIN")) {
//            return ordersRepository.findAll(pageable)
//                    .map(order -> mapToDTO(order, new OrdersDTO(), userIdUsernameMap));
//        }
//
//        // Дозволені філії для користувача
//        Set<String> allowedBranches = user.getAllowedBranches().stream()
//                .map(Branch::getName)
//                .collect(Collectors.toSet());
//
//        // Якщо роль — COUNTER, то показуємо всі замовлення по доступних філіях
//        if (userRoles.contains("ROLE_COUNTER")) {
//            return ordersRepository.findByBranchNameIn(allowedBranches, pageable)
//                    .map(order -> mapToDTO(order, new OrdersDTO(), userIdUsernameMap));
//        }
//
//        // Якщо requestedBranchName не входить до дозволених
//        if (!allowedBranches.contains(requestedBranchName)) {
//            throw new AccessDeniedException("Немає доступу до філіалу: " + requestedBranchName);
//        }
//
//        // Виводимо замовлення лише по requestedBranchName
//        return ordersRepository.findByBranchName(requestedBranchName, pageable)
//                .map(order -> mapToDTO(order, new OrdersDTO(), userIdUsernameMap));
//    }
    public Page<? extends BaseOrdersDTO> getOrders(Pageable pageable, String requestedBranchName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<String> userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Map<Long, String> userIdUsernameMap = userCacheService.loadMap();

        // ADMIN бачить усе
        if (userRoles.contains("ROLE_ADMIN")) {
            return ordersRepository.findAll(pageable)
                    .map(order -> mapToAdminDTO(order, userIdUsernameMap));
        }

        Set<String> allowedBranches = user.getAllowedBranches().stream()
                .map(Branch::getName)
                .collect(Collectors.toSet());

        // COUNTER бачить замовлення по дозволених філіях
        if (userRoles.contains("ROLE_COUNTER")) {
            return ordersRepository.findByBranchNameIn(allowedBranches, pageable)
                    .map(order -> mapToCounterDTO(order, userIdUsernameMap));
        }

        // USER — лише для одного філіалу
        if (!allowedBranches.contains(requestedBranchName)) {
            throw new AccessDeniedException("Немає доступу до філіалу: " + requestedBranchName);
        }

        return ordersRepository.findByBranchName(requestedBranchName, pageable)
                .map(order -> mapToUserDTO(order, userIdUsernameMap));
    }

    @Transactional
    public Long create(final OrdersDTO ordersDTO) {
        String username = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            username =  authentication.getName(); // Це і є username
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Orders order = new Orders();
        mapToEntity(ordersDTO, order);
        order.setEmpNo(user.getId()); // Присвоюємо userId як empNo
        ordersRepository.save(order);
        saveItems(1L, order, ordersDTO);
        return order.getOrderNo();
    }

    @Transactional
    public void update(final Long orderNo, final OrdersDTO ordersDTO) {
        final Orders existingOrder = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        mapToEntity(ordersDTO, existingOrder);
        ordersRepository.save(existingOrder);
        updateItems(existingOrder, ordersDTO);
    }

    public void delete(final Long orderNo) {
        ordersRepository.deleteById(orderNo);
    }


    private OrderAdminDTO mapToAdminDTO(Orders order, Map<Long, String> userMap) {
        OrderAdminDTO dto = new OrderAdminDTO();
        fillBaseFields(dto, order, userMap);
        dto.setTaxRate(order.getTaxRate());
        dto.setAmountPaid(order.getAmountPaid());
        dto.setAmountDueN(order.getAmountDueN());
        dto.setIncome(order.getIncome());
        dto.setTotalCost(order.getTotalCost());
        dto.setPriceLevel(order.getPriceLevel());
        return dto;
    }

    private OrderCounterDTO mapToCounterDTO(Orders order, Map<Long, String> userMap) {
        OrderCounterDTO dto = new OrderCounterDTO();
        fillBaseFields(dto, order, userMap);
        dto.setAmountPaid(order.getAmountPaid());
        dto.setAmountDueN(order.getAmountDueN());
        dto.setTotalCost(order.getTotalCost());
        return dto;
    }

    private OrderUserDTO mapToUserDTO(Orders order, Map<Long, String> userMap) {
        OrderUserDTO dto = new OrderUserDTO();
        fillBaseFields(dto, order, userMap);
        return dto;
    }

    private void fillBaseFields(final BaseOrdersDTO dto, final Orders order, final Map<Long, String> userMap) {
        dto.setOrderNo(order.getOrderNo());
        dto.setCustNo(order.getCustomer().getCustNo());
        dto.setCompany(order.getCustomer().getCompany());
        dto.setPhone(order.getCustomer().getMobile());
        dto.setBranchName(order.getBranch().getName());
        dto.setSaleDate(order.getSaleDate());
        dto.setShipDate(order.getShipDate());
        dto.setEmpNo(userMap.getOrDefault(order.getEmpNo(), null));
        dto.setShipToContact(order.getShipToContact());
        dto.setShipToAddr1(order.getShipToAddr1());
        dto.setShipToAddr2(order.getShipToAddr2());
        dto.setShipToCity(order.getShipToCity());
        dto.setShipToState(order.getShipToState());
        dto.setShipToZip(order.getShipToZip());
        dto.setShipToCountry(order.getShipToCountry());
        dto.setShipToPhone(order.getShipToPhone());
        dto.setShipVia(order.getShipVia());
        dto.setPo(order.getPo());
        dto.setTerms(order.getTerms());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setItemsTotal(order.getItemsTotal());
        dto.setFreight(order.getFreight());
        dto.setStatusOrder(order.getStatusOrder());
        dto.setRahFacNo(order.getRahFacNo());
        dto.setNotice(order.getNotice());
    }








    private OrdersDTO mapToDTO(final Orders orders, final OrdersDTO ordersDTO) {
        ordersDTO.setOrderNo(orders.getOrderNo());
        ordersDTO.setCustNo(orders.getCustomer().getCustNo());
        ordersDTO.setCompany(orders.getCustomer().getCompany());
        ordersDTO.setPhone(orders.getCustomer().getMobile());
//        ordersDTO.setItems(orders.getItems().stream()
//                .map(item -> itemsService.mapItemsToDTO(item, new ItemsDTO()))
//                .collect(Collectors.toList()));
        ordersDTO.setItems(itemsService.findByOrderNo(orders.getOrderNo()));
        ordersDTO.setBranchName(orders.getBranch().getName());
        ordersDTO.setSaleDate(orders.getSaleDate());
        ordersDTO.setShipDate(orders.getShipDate());

        User user = userRepository.findById(orders.getEmpNo()).orElse(null);
        if (user != null) {
            ordersDTO.setEmpNo(user.getUsername()); // записуємо username в DTO
        } else {
            ordersDTO.setEmpNo(null);
        }
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
        ordersDTO.setTotalCost(orders.getTotalCost());
        ordersDTO.setPriceLevel(orders.getPriceLevel());
        ordersDTO.setStatusOrder(orders.getStatusOrder());
        ordersDTO.setRahFacNo(orders.getRahFacNo());
        ordersDTO.setNotice(orders.getNotice());
        return ordersDTO;
    }

    private OrdersDTO mapToDTO(final Orders orders, final OrdersDTO ordersDTO, final Map<Long, String> userMap) {
        ordersDTO.setOrderNo(orders.getOrderNo());
        ordersDTO.setCustNo(orders.getCustomer().getCustNo());
        ordersDTO.setCompany(orders.getCustomer().getCompany());
        ordersDTO.setPhone(orders.getCustomer().getMobile());
        ordersDTO.setBranchName(orders.getBranch().getName());
        ordersDTO.setSaleDate(orders.getSaleDate());
        ordersDTO.setShipDate(orders.getShipDate());
        ordersDTO.setEmpNo(userMap.getOrDefault(orders.getEmpNo(), null));
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
        ordersDTO.setTotalCost(orders.getTotalCost());
        ordersDTO.setPriceLevel(orders.getPriceLevel());
        ordersDTO.setStatusOrder(orders.getStatusOrder());
        ordersDTO.setRahFacNo(orders.getRahFacNo());
        ordersDTO.setNotice(orders.getNotice());
        return ordersDTO;
    }

    private void mapToEntity(final OrdersDTO ordersDTO, final Orders orders) {
        Customer customer;
        if (ordersDTO.getCustNo() != null) {
            customer = customerRepository.findById(ordersDTO.getCustNo())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        } else {
            // Створюємо нового клієнта
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setCompany(ordersDTO.getCompany());
            customerDTO.setMobile(ordersDTO.getPhone());
            customerDTO.setAddr1(ordersDTO.getAddr1());

            Long newCustNo = customerService.create(customerDTO);
            customer = customerRepository.findById(newCustNo)
                    .orElseThrow(() -> new RuntimeException("Newly created customer not found"));
        }

        Branch branch = branchRepository.findByName(ordersDTO.getBranchName())
                .orElseThrow(() -> new RuntimeException("Branch not found: " + ordersDTO.getBranchName()));

        orders.setOrderNo(ordersDTO.getOrderNo());
        orders.setCustomer(customer);
        orders.setBranch(branch);
        orders.setSaleDate(ordersDTO.getSaleDate());
        orders.setShipDate(ordersDTO.getShipDate());
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
        orders.setTotalCost(ordersDTO.getTotalCost());
        orders.setPriceLevel(ordersDTO.getPriceLevel());
        orders.setStatusOrder(ordersDTO.getStatusOrder());
        orders.setRahFacNo(ordersDTO.getRahFacNo());
        orders.setNotice(ordersDTO.getNotice());
    }

    private void saveItems(Long itemNo, final Orders orders, final OrdersDTO ordersDTO) {
        if(ordersDTO.getItems() != null) {
            for (ItemsDTO itemDTO : ordersDTO.getItems()) {
                Items item = new Items();
                ItemId itemId = new ItemId(orders.getOrderNo(), itemNo);
                item.setId(itemId);
                item.setOrder(orders);
                item.setPartNo(itemDTO.getPartNo());
                item.setProfilWidth(itemDTO.getProfilWidth());
                item.setEstimationWidth(itemDTO.getEstimationWidth());
                item.setEstimationHeight(itemDTO.getEstimationHeight());
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
        Long maxItemNo = itemsRepository.findMaxItemNoByOrderNo(existingOrder.getOrderNo());
        Long nextItemNo = (maxItemNo != null ? maxItemNo : 0L) + 1;
        deleteAllItemsByOrderNo(existingOrder.getOrderNo());
        saveItems(nextItemNo, existingOrder, ordersDTO);
    }

    public void deleteAllItemsByOrderNo(Long orderNo) {
        itemsRepository.deleteByOrderOrderNo(orderNo);
    }

}
