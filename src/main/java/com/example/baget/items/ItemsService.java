package com.example.baget.items;

import com.example.baget.util.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class ItemsService {

    private final ItemsRepository itemsRepository;

    public ItemsService(final ItemsRepository itemsRepository) {
        this.itemsRepository = itemsRepository;
    }

    public List<ItemsDTO> findAll() {
        final List<Items> itemses = itemsRepository.findAll(Sort.by("order.orderNo"));
        return itemses.stream()
                .map(items -> mapToDTO(items, new ItemsDTO()))
                .toList();
    }

    public ItemsDTO get(final Long orderNo) {
        return itemsRepository.findById(orderNo)
                .map(items -> mapToDTO(items, new ItemsDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ItemsDTO itemsDTO) {
        final Items items = new Items();
        mapToEntity(itemsDTO, items);
        return itemsRepository.save(items).getOrder().getOrderNo();
    }

    public void update(final Long orderNo, final ItemsDTO itemsDTO) {
        final Items items = itemsRepository.findById(orderNo)
                .orElseThrow(NotFoundException::new);
        mapToEntity(itemsDTO, items);
        itemsRepository.save(items);
    }

    public void delete(final Long orderNo) {
        itemsRepository.deleteById(orderNo);
    }

    public ItemsDTO mapToDTO(final Items items, final ItemsDTO itemsDTO) {
        itemsDTO.setOrderNo(items.getOrder().getOrderNo());
        itemsDTO.setItemNo(items.getItemNo());
        itemsDTO.setPartNo(items.getPartNo());
        itemsDTO.setProfilWidth(items.getProfilWidth());
        itemsDTO.setWidth(items.getWidth());
        itemsDTO.setHeight(items.getHeight());
        itemsDTO.setQty(items.getQty());
        itemsDTO.setQuantity(items.getQuantity());
        itemsDTO.setSellPrice(items.getSellPrice());
        itemsDTO.setDiscount(items.getDiscount());
        itemsDTO.setOnHand(items.getOnHand());
        itemsDTO.setCost(items.getCost());
        return itemsDTO;
    }

    public Items mapToEntity(final ItemsDTO itemsDTO, final Items items) {
        items.setItemNo(itemsDTO.getItemNo());
        items.setPartNo(itemsDTO.getPartNo());
        items.setProfilWidth(itemsDTO.getProfilWidth());
        items.setWidth(itemsDTO.getWidth());
        items.setHeight(itemsDTO.getHeight());
        items.setQty(itemsDTO.getQty());
        items.setQuantity(itemsDTO.getQuantity());
        items.setSellPrice(itemsDTO.getSellPrice());
        items.setDiscount(itemsDTO.getDiscount());
        items.setOnHand(itemsDTO.getOnHand());
        items.setCost(itemsDTO.getCost());
        return items;
    }

}
