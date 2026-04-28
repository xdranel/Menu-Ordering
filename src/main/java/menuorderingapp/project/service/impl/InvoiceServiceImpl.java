package menuorderingapp.project.service.impl;

import menuorderingapp.project.model.*;
import menuorderingapp.project.repository.CashierRepository;
import menuorderingapp.project.repository.InvoiceRepository;
import menuorderingapp.project.repository.OrderRepository;
import menuorderingapp.project.service.InvoiceService;
import menuorderingapp.project.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final CashierRepository cashierRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              OrderRepository orderRepository,
                              CashierRepository cashierRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.cashierRepository = cashierRepository;
    }

    @Override
    public Invoice generateInvoice(Order order, Long cashierId) {
        Cashier cashier = null;
        if (cashierId != null) {
            cashier = cashierRepository.findById(cashierId)
                    .orElseThrow(() -> new RuntimeException("Cashier not found with id: " + cashierId));
        }

        Optional<Invoice> existingInvoice = invoiceRepository.findByOrder(order);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setCashier(cashier);
        invoice.setTotalAmount(order.getTotal());
        invoice.setTaxAmount(calculateTax(order.getTotal()));
        invoice.setFinalAmount(invoice.getTotalAmount().add(invoice.getTaxAmount()));

        Order.PaymentMethod paymentMethod = order.getPaymentMethod();
        if (paymentMethod == null) {
            paymentMethod = Order.PaymentMethod.CASH;
        }
        invoice.setPaymentMethod(paymentMethod);

        invoice.setCreatedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findByIdWithOrderAndCashier(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceByOrder(Order order) {
        return invoiceRepository.findByOrder(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByDateRange(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = LocalDate.parse(startDate, formatter).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate, formatter).atTime(23, 59, 59);

        return invoiceRepository.findInvoicesByDateRange(start, end);
    }

    @Override
    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));

        String pdfContent = generateInvoiceContent(invoice);
        return pdfContent.getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    private BigDecimal calculateTax(java.math.BigDecimal amount) {
        return amount.multiply(java.math.BigDecimal.valueOf(Constants.TAX_RATE));
    }

    private String generateInvoiceContent(Invoice invoice) {
        StringBuilder content = new StringBuilder();
        content.append("INVOICE: ").append(invoice.getInvoiceNumber()).append("\n");
        content.append("Date: ").append(invoice.getCreatedAt()).append("\n");
        content.append("Cashier: ").append(invoice.getCashier() != null ? invoice.getCashier().getDisplayName() : "Self-Service").append("\n");
        content.append("Order: ").append(invoice.getOrder().getOrderNumber()).append("\n");
        content.append("Customer: ").append(invoice.getOrder().getCustomerName()).append("\n");
        content.append("Payment Method: ").append(invoice.getPaymentMethod()).append("\n\n");
        content.append("Items:\n");

        for (OrderItem item : invoice.getOrder().getOrderItems()) {
            content.append(String.format("- %s x%d = %s\n",
                    item.getMenu().getName(),
                    item.getQuantity(),
                    item.getSubtotal()));
        }

        content.append("\nSubtotal: ").append(invoice.getTotalAmount()).append("\n");
        content.append("Tax: ").append(invoice.getTaxAmount()).append("\n");
        content.append("Total: ").append(invoice.getFinalAmount()).append("\n");

        return content.toString();
    }
}
