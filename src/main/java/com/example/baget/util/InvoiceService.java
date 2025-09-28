package com.example.baget.util;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private final OrdersRepository ordersRepository;

    public InvoiceService(OrdersRepository ordersRepository) {
        this.ordersRepository = ordersRepository;
    }

    public byte[] generateInvoicePdf(Long invoiceNo) {
        List<Orders> orders = ordersRepository.findByRahFacNo(invoiceNo);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Invoice " + invoiceNo + " not found");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);

        // Шапка постачальника
        document.add(new Paragraph("Постачальник:  ФОП Петров Валерій Афрікановіч", bold));
        document.add(new Paragraph("ЄДРПОУ 2512702072, тел. (063)433-28-91", normal));
        document.add(new Paragraph("Р/р UA893220010000026005320012345 в АТ «УНІВЕРСАЛ БАНК» МФО 322001", normal));
        document.add(new Paragraph("ІПН 15224372 № свідоцтва 8768534", normal));
        document.add(new Paragraph("Не є платником податку на прибуток на загальних підставах", normal));
        document.add(new Paragraph("Адреса: 54020, Миколаїв, Велика, 223/98\n\n", normal));

        // Одержувач
        document.add(new Paragraph("Одержувач: " + orders.get(0).getCustomer().getCompany(), bold));
        document.add(new Paragraph("Платник: той самий\n\n", normal));

        // Реквізити рахунку
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("uk")));
        document.add(new Paragraph("Рахунок-фактура № " + invoiceNo, bold));
        document.add(new Paragraph("від " + dateStr + "\n\n", normal));

        // Таблиця
        PdfPTable table = new PdfPTable(new float[]{1, 2, 5, 2, 3, 3});
        table.setWidthPercentage(100);

        table.addCell("№");
        table.addCell("№ замов.");
        table.addCell("Товар, послуга");
        table.addCell("Кількість");
        table.addCell("Ціна без ПДВ, грн");
        table.addCell("Сума без ПДВ, грн");

        int idx = 1;
        double total = 0.0;
        for (Orders o : orders) {
            double price = o.getAmountDueN() + o.getAmountPaid(); // приклад

            table.addCell(String.valueOf(idx++));
            table.addCell(String.valueOf(o.getOrderNo()));
            table.addCell("Виготовлення багетної рами");
            table.addCell("1");
            table.addCell(String.format("%.2f", price));
            table.addCell(String.format("%.2f", price));

            total += price;
        }

        document.add(table);

        // Підсумок
        document.add(new Paragraph("\nВсього: " + String.format("%.2f грн", total), bold));
        document.add(new Paragraph("Всього на суму: " + MoneyToWordsUA.convert(total), normal));
        document.add(new Paragraph("ПДВ: —\n\n", normal));
        document.add(new Paragraph("Виписав(ла): Петров В.А.", normal));

        document.close();

        return baos.toByteArray();
    }
}
