package com.example.baget.calculation.product;

import com.example.baget.calculation.preview.*;
import com.example.baget.calculation.suspension.*;
import com.example.baget.parts.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Internal composer called inside ProductPreviewService's read-only transaction. */
@Component
public class MirrorAccessoriesComposer {
    private final PartsRepository parts;
    private final MaterialPreviewService materials;
    private final SuspensionPreviewService suspensions;

    public MirrorAccessoriesComposer(PartsRepository parts, MaterialPreviewService materials,
                                    SuspensionPreviewService suspensions) {
        this.parts = parts; this.materials = materials; this.suspensions = suspensions;
    }

    public List<ProductPreviewResponse.Line> compose(ProductPreviewRequest r, Authentication auth) {
        var lines = new ArrayList<ProductPreviewResponse.Line>();
        if (r.backingPartNo() != null) {
            if (r.backingPartNo() != SuspensionRules.DVP)
                throw new PreviewException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_BACKING_PART", "Наразі підтримується ДВП 4460");
            var backing = parts.findById(r.backingPartNo()).orElseThrow(() -> new PreviewException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPOSITION_PART_MISSING", "Не знайдено ДВП 4460"));
            if (backing.getPartKind() != PartKind.MATERIAL || backing.getUnitType() != UnitType.SQUARE_METER
                    || backing.getCalculationMethod() != CalculationMethod.AREA
                    || backing.getCalculationParams() == null || !backing.getCalculationParams().isEmpty())
                throw new PreviewException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_BACKING_SETTINGS",
                        "Перевірте налаштування площі ДВП 4460");
            var result = materials.preview(new MaterialPreviewRequest(r.backingPartNo(), r.widthMm(), r.heightMm(),
                    r.productQuantity(), 1), auth);
            lines.add(new ProductPreviewResponse.Line("BACKING", "SELECTED", result));
        }
        if (r.suspension() != null) {
            var choice = r.suspension();
            // Never accept another width, quantity or hasMirror/hasDvp from nested suspension JSON.
            var result = suspensions.preview(new SuspensionPreviewRequest(r.widthMm(), r.productQuantity(),
                    r.backingPartNo(), r.mirrorPartNo(), choice.hangerMode(), choice.hangerPartNo(),
                    choice.hangersPerProduct(), choice.includePozzi(), choice.pozziPerProduct()), auth);
            lines.addAll(result.lines());
        }
        return List.copyOf(lines);
    }
}
