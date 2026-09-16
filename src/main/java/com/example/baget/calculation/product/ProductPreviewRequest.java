package com.example.baget.calculation.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductPreviewRequest(
        @NotNull ProductType productType,
        @NotNull @Positive @DecimalMax("100000") @Digits(integer=6, fraction=3) BigDecimal widthMm,
        @NotNull @Positive @DecimalMax("100000") @Digits(integer=6, fraction=3) BigDecimal heightMm,
        @NotNull @Min(1) @Max(10000) Integer productQuantity,
        @NotNull @Positive Long framePartNo,
        @NotNull @Positive Long mirrorPartNo
) {
    public enum ProductType { MIRROR_IN_FRAME }
}
