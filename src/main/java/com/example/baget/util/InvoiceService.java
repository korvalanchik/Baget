package com.example.baget.util;

import com.example.baget.my_company.CompanyDetails;
import com.example.baget.my_company.CompanyDetailsService;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.lowagie.text.*;
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
    private final CompanyDetailsService companyDetailsService;

    public InvoiceService(OrdersRepository ordersRepository, CompanyDetailsService companyDetailsService) {
        this.ordersRepository = ordersRepository;
        this.companyDetailsService = companyDetailsService;
    }

    public byte[] generateInvoicePdf(Long invoiceNo) {
        List<Orders> orders = ordersRepository.findByRahFacNo(invoiceNo);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("Invoice " + invoiceNo + " not found");
        }

        CompanyDetails details = companyDetailsService.get(); // ← МАГІЯ

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);

        // --- РЕКВІЗИТИ БЕРЕМО З БД ---
        document.add(new Paragraph("Постачальник: " + details.getFullName(), bold));
        if(details.getEdrpou() != null)
            document.add(new Paragraph("ЄДРПОУ " + details.getEdrpou() + ", тел. " + details.getPhone(), normal));
        if(details.getBankAccount() != null)
            document.add(new Paragraph(details.getBankAccount(), normal));
        if(details.getIpn() != null)
            document.add(new Paragraph("ІПН " + details.getIpn(), normal));
        if (details.getComment() != null)
            document.add(new Paragraph(details.getComment(), normal));
        if(details.getAddress() != null)
            document.add(new Paragraph("Адреса: " + details.getAddress() + "\n\n", normal));


//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
//        PdfWriter.getInstance(document, baos);
//        document.open();
//
//        Font bold = new Font(Font.HELVETICA, 10, Font.BOLD);
//        Font normal = new Font(Font.HELVETICA, 10, Font.NORMAL);
//
//        // Шапка постачальника
//        document.add(new Paragraph("Постачальник:  ФОП Петров Валерій Афрікановіч", bold));
//        document.add(new Paragraph("ЄДРПОУ 2512702072, тел. (063)433-28-91", normal));
//        document.add(new Paragraph("Р/р UA893220010000026005320012345 в АТ «УНІВЕРСАЛ БАНК» МФО 322001", normal));
//        document.add(new Paragraph("ІПН 15224372 № свідоцтва 8768534", normal));
//        document.add(new Paragraph("Не є платником податку на прибуток на загальних підставах", normal));
//        document.add(new Paragraph("Адреса: 54020, Миколаїв, Велика, 223/98\n\n", normal));

        // Одержувач
        document.add(new Paragraph("Одержувач: " + resolveCustomerName(orders), bold));
        document.add(new Paragraph("Платник: той самий\n\n", normal));

        // Реквізити рахунку
//        LocalDate today = LocalDate.now();
        LocalDate invoiceDate = extractDateFromInvoiceNo(invoiceNo);
        String dateStr = invoiceDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new java.util.Locale("uk")));

        Paragraph p = new Paragraph("Рахунок-фактура № " + invoiceNo, bold);
        p.setAlignment(Element.ALIGN_CENTER);
        document.add(p);

        Paragraph d = new Paragraph("від " + dateStr, normal);
        d.setAlignment(Element.ALIGN_CENTER);
        document.add(d);

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
            table.addCell(details.getWorkTitle());
            table.addCell("1");
            table.addCell(String.format("%.2f", price));
            table.addCell(String.format("%.2f", price));

            total += price;
        }

        document.add(table);

        // Підсумок
        Paragraph s = new Paragraph("\nВсього: " + String.format("%.2f грн", total), bold);
        s.setAlignment(Element.ALIGN_RIGHT);
        document.add(s);
        document.add(new Paragraph("Всього на суму: " + MoneyToWordsUA.convert(total), bold));
        document.add(new Paragraph("ПДВ: —\n\n", normal));
        document.add(new Paragraph("Виписав(ла): " + details.getInitials(), normal));

        document.close();

        return baos.toByteArray();
    }

    private String resolveCustomerName(List<Orders> orders) {

        // Витягаємо першого клієнта
        Long firstCustomerId = orders.get(0).getCustomer().getCustNo();

        boolean allSame = orders.stream()
                .allMatch(o -> o.getCustomer().getCustNo().equals(firstCustomerId));

        if (allSame) {
            return orders.get(0).getCustomer().getCompany();
        }

        // Інакше — компанія за замовчуванням
        return companyDetailsService.get().getDefaultRecipient();
    }

    private LocalDate extractDateFromInvoiceNo(Long invoiceNo) {
        String s = invoiceNo.toString();

        if (s.length() < 9) {
            throw new IllegalArgumentException("Invalid invoice number: " + s);
        }
        int len = s.length();
        int year = Integer.parseInt(s.substring(len - 7, len - 3));
        int month = Integer.parseInt(s.substring(len - 9, len - 7));
        int day = Integer.parseInt(s.substring(0, len - 9));

        return LocalDate.of(year, month, day);
    }


}
