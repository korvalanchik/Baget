package com.example.baget.parts;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/partss", produces = MediaType.APPLICATION_JSON_VALUE)
public class PartsResource {

    private final PartsService partsService;

    public PartsResource(final PartsService partsService) {
        this.partsService = partsService;
    }

    @GetMapping
    public ResponseEntity<List<PartsDTO>> getAllPartss() {
        return ResponseEntity.ok(partsService.findAll());
    }

    @GetMapping("/{partNo}")
    public ResponseEntity<PartsDTO> getParts(@PathVariable(name = "partNo") final Long partNo) {
        return ResponseEntity.ok(partsService.get(partNo));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<Long> createParts(@RequestBody @Valid final PartsDTO partsDTO) {
        final Long createdPartNo = partsService.create(partsDTO);
        return new ResponseEntity<>(createdPartNo, HttpStatus.CREATED);
    }

    @PutMapping("/{partNo}")
    public ResponseEntity<Long> updateParts(@PathVariable(name = "partNo") final Long partNo,
            @RequestBody @Valid final PartsDTO partsDTO) {
        partsService.update(partNo, partsDTO);
        return ResponseEntity.ok(partNo);
    }

    @DeleteMapping("/{partNo}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteParts(@PathVariable(name = "partNo") final Long partNo) {
        partsService.delete(partNo);
        return ResponseEntity.noContent().build();
    }

}
