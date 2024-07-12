package com.example.baget.orders;

import com.example.baget.util.WebUtils;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/orderss")
public class OrdersController {

    private final OrdersService ordersService;

    public OrdersController(final OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("orderses", ordersService.findAll());
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
