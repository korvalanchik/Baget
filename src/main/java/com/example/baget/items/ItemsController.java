package com.example.baget.items;

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
@RequestMapping("/items")
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(final ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping
    public String list(final Model model) {
        model.addAttribute("itemses", itemsService.findAll());
        return "items/list";
    }

    @GetMapping("/add")
    public String add(@ModelAttribute("items") final ItemsDTO itemsDTO) {
        return "items/add";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute("items") @Valid final ItemsDTO itemsDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "items/add";
        }
        itemsService.create(itemsDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("items.create.success"));
        return "redirect:/items";
    }

    @GetMapping("/edit/{orderNo}")
    public String edit(@PathVariable(name = "orderNo") final Long orderNo, final Model model) {
        model.addAttribute("items", itemsService.get(orderNo));
        return "items/edit";
    }

    @PostMapping("/edit/{orderNo}")
    public String edit(@PathVariable(name = "orderNo") final Long orderNo,
            @ModelAttribute("items") @Valid final ItemsDTO itemsDTO,
            final BindingResult bindingResult, final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "items/edit";
        }
        itemsService.update(orderNo, itemsDTO);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_SUCCESS, WebUtils.getMessage("items.update.success"));
        return "redirect:/items";
    }

    @PostMapping("/delete/{orderNo}")
    public String delete(@PathVariable(name = "orderNo") final Long orderNo,
            final RedirectAttributes redirectAttributes) {
        itemsService.delete(orderNo);
        redirectAttributes.addFlashAttribute(WebUtils.MSG_INFO, WebUtils.getMessage("items.delete.success"));
        return "redirect:/items";
    }

}
