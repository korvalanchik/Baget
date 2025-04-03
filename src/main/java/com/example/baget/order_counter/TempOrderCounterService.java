package com.example.baget.order_counter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TempOrderCounterService {
    private final TempOrderCounterRepository tempOrderCounterRepository;

    public TempOrderCounterService(TempOrderCounterRepository tempOrderCounterRepository) {
        this.tempOrderCounterRepository = tempOrderCounterRepository;
    }

    // Метод для отримання наступного номера замовлення
    @Transactional
    public Long getNextOrderNumber() {
        // Знайдемо лічильник
        TempOrderCounter counter = tempOrderCounterRepository.findById(1).orElseThrow(() -> new RuntimeException("Counter not found"));

        // Інкрементуємо номер замовлення
        Long nextOrderNo = counter.getLastTempOrderNo() + 1;

        // Оновлюємо номер в таблиці
        counter.setLastTempOrderNo(nextOrderNo);
        tempOrderCounterRepository.save(counter);

        // Повертаємо новий номер
        return nextOrderNo;
    }

}
