package com.example.baget.calculation.product;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;

public record ProductPreviewRequest(
        @NotNull ProductType productType,
        @NotNull @Positive @DecimalMax("100000") @Digits(integer=6, fraction=3) BigDecimal widthMm,
        @NotNull @Positive @DecimalMax("100000") @Digits(integer=6, fraction=3) BigDecimal heightMm,
        @NotNull @Min(1) @Max(10000) Integer productQuantity,
        @Positive Long framePartNo,
        @Positive Long mirrorPartNo,
        @Positive Long underframePartNo,
        @Positive Long backingPartNo,
        @Valid SuspensionSelection suspension
) {
    public enum ProductType { MIRROR_IN_FRAME, UNDERFRAME }

    public ProductPreviewRequest(ProductType type, BigDecimal width, BigDecimal height,
                                 Integer quantity, Long frame, Long mirror, Long underframe) {
        this(type, width, height, quantity, frame, mirror, underframe, null, null);
    }

    // Preserve existing Java callers; mirror HTTP JSON also remains valid.
    public ProductPreviewRequest(ProductType type, BigDecimal width, BigDecimal height,
                                 Integer quantity, Long frame, Long mirror) {
        this(type, width, height, quantity, frame, mirror, null);
    }
}
