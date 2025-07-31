package com.example.baget.parts;

import com.example.baget.util.CustomOptimisticLockException;
import com.example.baget.util.NotFoundException;
import com.example.baget.vendors.VendorsRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
public class PartsService {

    private final PartsRepository partsRepository;
    private final VendorsRepository vendorsRepository;
    private final JdbcTemplate jdbcTemplate;

    public PartsService(final PartsRepository partsRepository, VendorsRepository vendorsRepository, JdbcTemplate jdbcTemplate) {
        this.partsRepository = partsRepository;
        this.vendorsRepository = vendorsRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<PartsDTO> findAll() {
        final List<Parts> parts = partsRepository.findAll(Sort.by("partNo"));
        return parts.stream()
                .map(part -> mapToDTO(part, new PartsDTO()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PartsDTO get(final Long partNo) {
        return partsRepository.findById(partNo)
                .map(parts -> mapToDTO(parts, new PartsDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<PartsDTO> getParts(Pageable pageable) {
        // Виклик репозиторію для отримання сторінки замовлень
        return partsRepository.findAll(pageable)
                .map(parts -> mapToDTO(parts, new PartsDTO())); // Перетворення у DTO, якщо потрібно
    }

    @Transactional
    public Long create(final PartsDTO partsDTO) {
        final Parts parts = new Parts();
        mapToEntity(partsDTO, parts);
        return partsRepository.save(parts).getPartNo();
    }

    @Transactional
    public void update(final Long partNo, final PartsDTO partsDTO) {
        try {
            final Parts parts = partsRepository.findById(partNo)
                    .orElseThrow(NotFoundException::new);
            mapToEntity(partsDTO, parts);
            partsRepository.save(parts);
        } catch (OptimisticLockingFailureException ex) {
            throw new CustomOptimisticLockException("The record was updated or deleted by another transaction");
        }

    }

    @Transactional
    public void delete(final Long partNo) {
        partsRepository.deleteById(partNo);
    }

    public PartsDTO mapToDTO(final Parts parts, final PartsDTO partsDTO) {
        partsDTO.setVendorNo(parts.getVendor() != null ? parts.getVendor().getVendorNo() : null);
        partsDTO.setVendorName(parts.getVendor() != null ? parts.getVendor().getVendorName() : null);

        partsDTO.setPartNo(parts.getPartNo());
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
        partsDTO.setVersion(parts.getVersion());

        return partsDTO;
    }

    public void mapToEntity(final PartsDTO partsDTO, final Parts parts) {
        parts.setVendor(vendorsRepository.findById(partsDTO.getVendorNo()).orElse(null));

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
        parts.setVersion(partsDTO.getVersion());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getBaget() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            List<Parts> parts = partsRepository.findAll(Sort.by("partNo"));
            List<PartsDTO> dtoList = parts.stream()
                    .map(part -> mapToDTO(part, new PartsDTO()))
                    .toList();
            return ResponseEntity.ok(dtoList);
        }

        String priceColumn = resolvePriceColumn(authorities);
        validatePriceColumn(priceColumn);

        List<ProfilListDTO> profileParts = fetchProfileParts(priceColumn);
        List<AccessoryListDTO> accessoryParts = fetchAccessoryParts(priceColumn);

        BagetResponseDTO response = new BagetResponseDTO(profileParts, accessoryParts);
        return ResponseEntity.ok(response);
    }

    private String resolvePriceColumn(Collection<? extends GrantedAuthority> authorities) {
        if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_COUNTER"))) {
            return "ListPrice_1";
        } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_LEVEL2"))) {
            return "ListPrice";
        } else {
            return "ListPrice_3"; // Default for ROLE_USER, ROLE_SELLER, etc.
        }
    }

    private void validatePriceColumn(String priceColumn) {
        Set<String> allowedColumns = Set.of("ListPrice", "ListPrice_1", "ListPrice_3");
        if (!allowedColumns.contains(priceColumn)) {
            throw new IllegalArgumentException("Invalid price column: " + priceColumn);
        }
    }

    private List<ProfilListDTO> fetchProfileParts(String priceColumn) {
        String sql = String.format("""
            SELECT PartNo, Description, ProfilWidth, InQuality, OnHand, %s AS ListPrice
            FROM parts
            WHERE InQuality = 2 AND ProfilWidth > 0.0003
            ORDER BY ProfilWidth ASC
            """, priceColumn);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProfilListDTO(
                rs.getLong("partNo"),
                rs.getString("description"),
                Math.round(rs.getDouble("profilWidth") * 1000), // mm
                rs.getDouble("onHand"),
                rs.getDouble("listPrice")
        ));
    }

    private List<AccessoryListDTO> fetchAccessoryParts(String priceColumn) {
        String sql = String.format("""
            SELECT PartNo, Description, %s AS ListPrice
            FROM parts
            WHERE ProfilWidth < 0.0003 OR ProfilWidth IS NULL
            """, priceColumn);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AccessoryListDTO(
                rs.getLong("partNo"),
                rs.getString("description"),
                rs.getDouble("listPrice")
        ));
    }

}
