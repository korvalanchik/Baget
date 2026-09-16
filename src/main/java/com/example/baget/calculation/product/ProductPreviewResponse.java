package com.example.baget.calculation.product;

import com.example.baget.calculation.preview.MaterialPreviewResponse;
import java.math.BigDecimal;
import java.util.List;

public record ProductPreviewResponse(
        ProductPreviewRequest.ProductType productType,
        int productQuantity,
        List<Line> lines,
        BigDecimal linesSubtotal,
        BigDecimal roundingAdjustment,
        BigDecimal total,
        String currency
) {
    public record Line(String role, String source, MaterialPreviewResponse calculation) {}
}
