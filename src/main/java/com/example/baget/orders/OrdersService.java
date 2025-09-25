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
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final UsersRepository userRepository;
    private final BranchRepository branchRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsService itemsService;
    public OrdersService(final OrdersRepository ordersRepository, CustomerRepository customerRepository,
                         CustomerService customerService, UsersRepository userRepository,
                         BranchRepository branchRepository,
                         ItemsRepository itemsRepository, ItemsService itemsService) {
        this.ordersRepository = ordersRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.userRepository = userRepository;
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

// previous
//    public Page<? extends BaseOrdersDTO> getOrders(Pageable pageable, String requestedBranchName) {
//        UserAndRoles result = getUserAndRoles();
//
//        Map<Long, String> userIdUsernameMap = userCacheService.loadMap();
//
//        // ADMIN бачить усе
//        if (result.userRoles().contains("ROLE_ADMIN")) {
//            return ordersRepository.findAll(pageable)
//                    .map(order -> mapToAdminDTO(order, userIdUsernameMap));
//        }
//
//        Set<String> allowedBranches = result.user().getAllowedBranches().stream()
//                .map(Branch::getName)
//                .collect(Collectors.toSet());
//
//        // COUNTER бачить замовлення по дозволених філіях
//        if (result.userRoles().contains("ROLE_COUNTER")) {
//            return ordersRepository.findByBranchNameIn(allowedBranches, pageable)
//                    .map(order -> mapToCounterDTO(order, userIdUsernameMap));
//        }
//
//        // USER — лише для одного філіалу
//        if (!allowedBranches.contains(requestedBranchName)) {
//            throw new AccessDeniedException("Немає доступу до філіалу: " + requestedBranchName);
//        }
//
//        return ordersRepository.findByBranch_Name(requestedBranchName, pageable)
//                .map(order -> mapToUserDTO(order, userIdUsernameMap));
//    }
    public Page<? extends OrderProjections.BaseOrdersView> getOrders(Pageable pageable, String requestedBranchName) {
        UserAndRoles userAndRoles = getUserAndRoles();

        Set<String> allowedBranches = userAndRoles.user().getAllowedBranches()
                .stream()
                .map(Branch::getName)
                .collect(Collectors.toSet());

        if (userAndRoles.userRoles().contains("ROLE_ADMIN")) {
            return ordersRepository.findAllAdminBy(pageable);
        }

        if (userAndRoles.userRoles().contains("ROLE_COUNTER")) {
            return ordersRepository.findByBranch_NameIn(allowedBranches, pageable);
        }

        // USER — лише для конкретної філії
        if (!allowedBranches.contains(requestedBranchName)) {
            throw new AccessDeniedException("Немає доступу до філіалу: " + requestedBranchName);
        }

        return ordersRepository.findByBranch_Name(requestedBranchName, pageable);
    }

    private UserAndRoles getUserAndRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<String> userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserAndRoles(user, userRoles);
    }

    private record UserAndRoles(User user, Set<String> userRoles) {
    }


    public Page<OrderSummaryView> getOrderSummaries(Pageable pageable) {
        UserAndRoles result = getUserAndRoles();

        // ADMIN бачить усе
        if (result.userRoles.contains("ROLE_ADMIN")) {
            return ordersRepository.findAllSummaryBy(pageable);
        } else {
            throw new AccessDeniedException("Обмежено права доступу");
        }
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
        order.setEmployee(user);
//        order.setEmpNo(user.getId()); // Присвоюємо userId як empNo
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

        User user = orders.getEmployee();
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

    private void mapToEntity(final OrdersDTO ordersDTO, final Orders orders) {
        Customer customer;
        if (ordersDTO.getCustNo() != null) {
            customer = customerRepository.findById(ordersDTO.getCustNo())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        } else {

            customer = customerRepository.findFirstByMobileContainingOrderByCustNoAsc(ordersDTO.getPhone())
                    .orElseGet(() -> {
                        // Якщо не знайшли — створюємо нового
                        CustomerDTO customerDTO = new CustomerDTO();
                        customerDTO.setCompany(ordersDTO.getCompany());
                        customerDTO.setMobile(ordersDTO.getPhone());
                        customerDTO.setAddr1(ordersDTO.getAddr1());

                        Long newCustNo = customerService.create(customerDTO);
                        return customerRepository.findById(newCustNo)
                                .orElseThrow(() -> new RuntimeException("Newly created customer not found"));
                    });
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
                item.setResellerPrice(itemDTO.getResellerPrice());
                item.setDiscount(itemDTO.getDiscount());
                item.setOnHand(itemDTO.getOnHand());
                item.setCost(itemDTO.getCost());
                item.setSum(itemDTO.getSum());

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
