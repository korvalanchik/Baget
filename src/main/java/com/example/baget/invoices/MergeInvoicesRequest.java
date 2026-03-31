package com.example.baget.invoices;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeInvoicesRequest {

    private List<Long> invoiceIds;   // 🔥 що об'єднуємо

    private Long payerId;            // 🔥 хто платить (optional)

    private String note;             // 🔥 коментар (optional)

    private boolean applyAdvance;    // 🔥 чи списувати аванс
}