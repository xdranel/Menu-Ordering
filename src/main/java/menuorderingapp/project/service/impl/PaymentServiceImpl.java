package menuorderingapp.project.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import menuorderingapp.project.model.Order;
import menuorderingapp.project.repository.OrderRepository;
import menuorderingapp.project.service.PaymentService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    public PaymentServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public String generateQRCode(String paymentData) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(paymentData, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    public boolean processQRPayment(String orderNumber, String qrData) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();

        try {
            Thread.sleep(1000);

            order.setPaymentMethod(Order.PaymentMethod.QR_CODE);
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean processCashPayment(String orderNumber, Double amountTendered) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();

        // Calculate final amount with 10% tax
        double subtotal = order.getTotal().doubleValue();
        double tax = subtotal * 0.10;
        double finalAmount = subtotal + tax;

        if (amountTendered < finalAmount) {
            return false;
        }

        order.setPaymentMethod(Order.PaymentMethod.CASH);
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return true;
    }

    @Override
    public String generatePaymentQRCode(Order order) {
        // Calculate final amount with 10% tax
        double subtotal = order.getTotal().doubleValue();
        double tax = subtotal * 0.10;
        double finalAmount = subtotal + tax;

        String paymentData = String.format(
                "order_number=%s&amount=%.2f&merchant=ChopChopRestaurant",
                order.getOrderNumber(),
                finalAmount
        );
        return generateQRCode(paymentData);
    }

    @Override
    public boolean verifyPayment(String orderNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        return orderOpt.isPresent() && orderOpt.get().getPaymentStatus() == Order.PaymentStatus.PAID;
    }
}
