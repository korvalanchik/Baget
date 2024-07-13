package com.example.baget.parts;

import com.example.baget.util.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class PartsService {

    private final PartsRepository partsRepository;

    public PartsService(final PartsRepository partsRepository) {
        this.partsRepository = partsRepository;
    }

    public List<PartsDTO> findAll() {
        final List<Parts> partses = partsRepository.findAll(Sort.by("partNo"));
        return partses.stream()
                .map(parts -> mapToDTO(parts, new PartsDTO()))
                .toList();
    }

    public PartsDTO get(final Long partNo) {
        return partsRepository.findById(partNo)
                .map(parts -> mapToDTO(parts, new PartsDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final PartsDTO partsDTO) {
        final Parts parts = new Parts();
        mapToEntity(partsDTO, parts);
        return partsRepository.save(parts).getPartNo();
    }

    public void update(final Long partNo, final PartsDTO partsDTO) {
        final Parts parts = partsRepository.findById(partNo)
                .orElseThrow(NotFoundException::new);
        mapToEntity(partsDTO, parts);
        partsRepository.save(parts);
    }

    public void delete(final Long partNo) {
        partsRepository.deleteById(partNo);
    }

    private PartsDTO mapToDTO(final Parts parts, final PartsDTO partsDTO) {
        partsDTO.setPartNo(parts.getPartNo());
        partsDTO.setVendorNo(parts.getVendorNo());
        partsDTO.setDescription(parts.getDescription());
        partsDTO.setProfilWidth(parts.getProfilWidth());
        partsDTO.setInQuality(parts.getInQuality());
        partsDTO.setOnHand(parts.getOnHand());
        partsDTO.setOnOrder(parts.getOnOrder());
        partsDTO.setCost(parts.getCost());
        partsDTO.setListPrice(parts.getListPrice());
        partsDTO.setListPrice1(parts.getListPrice_1());
        partsDTO.setListPrice2(parts.getListPrice_2());
        partsDTO.setNoPercent(parts.getNoPercent());
        partsDTO.setListPrice3(parts.getListPrice_3());
        return partsDTO;
    }

    private Parts mapToEntity(final PartsDTO partsDTO, final Parts parts) {
        parts.setVendorNo(partsDTO.getVendorNo());
        parts.setDescription(partsDTO.getDescription());
        parts.setProfilWidth(partsDTO.getProfilWidth());
        parts.setInQuality(partsDTO.getInQuality());
        parts.setOnHand(partsDTO.getOnHand());
        parts.setOnOrder(partsDTO.getOnOrder());
        parts.setCost(partsDTO.getCost());
        parts.setListPrice(partsDTO.getListPrice());
        parts.setListPrice_1(partsDTO.getListPrice1());
        parts.setListPrice_2(partsDTO.getListPrice2());
        parts.setNoPercent(partsDTO.getNoPercent());
        parts.setListPrice_3(partsDTO.getListPrice3());
        return parts;
    }

}
