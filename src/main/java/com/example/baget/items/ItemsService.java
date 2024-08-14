package com.example.baget.items;

import com.example.baget.orders.Orders;
import com.example.baget.util.NotFoundException;
import java.util.List;

import jakarta.transaction.Transactional;
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

    public List<ItemsDTO> findByOrderNo(Long orderNo) {
        final List<Items> items = itemsRepository.findByOrderOrderNo(orderNo);
        return items.stream()
                .map(item -> mapToDTO(item, new ItemsDTO()))
                .toList();
    }

    public ItemsDTO get(final Long orderNo, final Long itemNo) {
        ItemId itemId = new ItemId(orderNo, itemNo);
        return itemsRepository.findById(itemId)
                .map(items -> mapToDTO(items, new ItemsDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ItemsDTO itemsDTO) {
        Long orderNo = itemsDTO.getOrderNo();
        Long maxItemNo = itemsRepository.findMaxItemNoByOrderNo(orderNo);
        Long newItemNo = (maxItemNo != null) ? maxItemNo + 1 : 1L;
        itemsDTO.setItemNo(newItemNo);
        final Items items = new Items();
        mapToEntity(itemsDTO, items);
        return itemsRepository.save(items).getOrder().getOrderNo();
    }

    public void update(final Long orderNo, final Long itemNo, final ItemsDTO itemsDTO) {
        ItemId itemId = new ItemId(orderNo, itemNo);
        final Items items = itemsRepository.findById(itemId)
                .orElseThrow(NotFoundException::new);
        mapToEntity(itemsDTO, items);
        itemsRepository.save(items);
    }

    @Transactional
    public void delete(final Long orderNo, final Long itemNo) {
        itemsRepository.deleteItemByOrderNoAndItemNo(orderNo, itemNo);
    }

    public ItemsDTO mapToDTO(final Items items, final ItemsDTO itemsDTO) {
        if (items == null) {
            return null;
        }
        itemsDTO.setItemNo(items.getId().getItemNo());
        itemsDTO.setOrderNo(items.getId().getOrderNo());
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

    public static Items mapToEntity(final ItemsDTO itemsDTO, final Items items) {
        if (itemsDTO == null) {
            return null;
        }
        ItemId itemId = new ItemId(itemsDTO.getOrderNo(), itemsDTO.getItemNo());
        items.setId(itemId);
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
