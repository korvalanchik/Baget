package com.example.baget.calculation.product;

import com.example.baget.calculation.preview.*;
import com.example.baget.parts.*;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.math.BigDecimal;

@Service
public class ProductPreviewService {
    private final MirrorFrameRule rule;
    private final PartsRepository parts;
    private final MaterialPreviewService materials;
    private final PartPriceResolver prices;
    private final Validator validator;

    public ProductPreviewService(MirrorFrameRule rule, PartsRepository parts,
            MaterialPreviewService materials, PartPriceResolver prices, Validator validator) {
        this.rule = rule;
        this.parts = parts;
        this.materials = materials;
        this.prices = prices;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public ProductPreviewResponse preview(ProductPreviewRequest request, Authentication auth) {
        prices.requireAuthenticated(auth);
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new PreviewException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Перевірте параметри виробу");
        }
        var components = rule.compose(request);
        var lines = new ArrayList<ProductPreviewResponse.Line>();
        var amounts = new ArrayList<BigDecimal>();
        for (var component : components) {
            var part = parts.findById(component.partNo()).orElseThrow(() ->
                    new PreviewException(HttpStatus.UNPROCESSABLE_ENTITY, "COMPOSITION_PART_MISSING",
                            "Не знайдено матеріал " + component.partNo() + " для " + component.role()));
            var expected = switch (component.role()) {
                case "FRAME" -> CalculationMethod.FRAME_PROFILE;
                case "MIRROR" -> CalculationMethod.AREA;
                case "MIRROR_GLUE" -> CalculationMethod.PERIMETER;
                default -> throw new IllegalStateException("Unknown component role");
            };
            if (part.getPartKind() != PartKind.MATERIAL || part.getCalculationMethod() != expected) {
                throw new PreviewException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_COMPONENT_SETTINGS",
                        "Позиція " + component.partNo() + " не налаштована для " + component.role());
            }
            // Reuse tested validation, consumption and role-based pricing from step 3.
            var result = materials.preview(new MaterialPreviewRequest(component.partNo(),
                    request.widthMm(), request.heightMm(), request.productQuantity(), 1), auth);
            lines.add(new ProductPreviewResponse.Line(component.role(), component.source(), result));
            amounts.add(result.totalConsumption().multiply(result.unitPrice()));
        }
        var totals = ProductTotals.fromUnroundedAmounts(amounts);
        return new ProductPreviewResponse(request.productType(), request.productQuantity(),
                java.util.List.copyOf(lines), totals.linesSubtotal(), totals.roundingAdjustment(),
                totals.total(), "UAH");
    }
}
