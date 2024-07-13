package com.example.baget.orders;

import com.example.baget.util.WebUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Controller
@RequestMapping("/orderss")
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(final OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @GetMapping
//    public String list(final Model model) {
//        model.addAttribute("orderses", ordersService.findAll());
//        return "orders/list";
//    }

    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrdersDTO> ordersPage = ordersService.findAll(pageable);

        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ordersPage.getTotalPages());
        return "orders/list";
    }


    @GetMapping("/add")
    public String add(@ModelAttribute("orders") final OrdersDTO ordersDTO) {
        return "orders/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("orders") @Valid final OrdersDTO ordersDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "orders/add";
        }
        ordersService.create(ordersDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("orders.create.success"));
        return "redirect:/orderss";
    }

    @GetMapping("/edit/{orderNo}")
    public String edit(@PathVariable(name = "orderNo") final Long orderNo, final Model model) {
        model.addAttribute("orders", ordersService.get(orderNo));
        return "orders/edit";
    }

    @PostMapping("/edit/{orderNo}")
    public String edit(@PathVariable(name = "orderNo") final Long orderNo,
            @ModelAttribute("orders") @Valid final OrdersDTO ordersDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "orders/edit";
        }
        ordersService.update(orderNo, ordersDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("orders.update.success"));
        return "redirect:/orderss";
    }

    @PostMapping("/delete/{orderNo}")
    public String delete(@PathVariable(name = "orderNo") final Long orderNo,
            final RedirectAttributes redirectAttributes) {
        ordersService.delete(orderNo);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("orders.delete.success"));
        return "redirect:/orderss";
    }

}
