package com.voucherpro.service;

import com.voucherpro.dto.CreateOrderRequest;
import com.voucherpro.dto.OrderResponse;
import com.voucherpro.dto.SubmitUtrRequest;
import com.voucherpro.model.Order;
import com.voucherpro.model.PaymentStatus;
import com.voucherpro.model.User;
import com.voucherpro.model.Voucher;
import com.voucherpro.repository.OrderRepository;
import com.voucherpro.repository.UserRepository;
import com.voucherpro.repository.VoucherRepository;
import com.voucherpro.util.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;

    @Value("${app.payment.upi-id}")
    private String upiId;

    @Value("${app.payment.merchant-name}")
    private String merchantName;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            VoucherRepository voucherRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.voucherRepository = voucherRepository;
    }

    public OrderResponse createOrder(String email, CreateOrderRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        Voucher voucher = voucherRepository.findById(request.getVoucherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found."));

        String orderId = "ORD" + System.currentTimeMillis();
        double amount = voucher.getDiscountPrice();

        String upiLink;
        try {
            String encodedMerchantName = URLEncoder.encode(merchantName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            upiLink = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=%s",
                    upiId,
                    encodedMerchantName,
                    amount,
                    orderId
            );
        } catch (UnsupportedEncodingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "UPI Link Generation failed due to encoding issues.");
        }

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(user.getId());
        order.setVoucherId(voucher.getId());
        order.setAmount(amount);
        order.setUpiLink(upiLink);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        Order saved = orderRepository.save(order);
        return mapToOrderResponse(saved);
    }

    public OrderResponse getOrderDetails(String email, String orderId) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        // Only the user who placed it or an ADMIN can view it
        if (!order.getUserId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to view this order.");
        }

        return mapToOrderResponse(order);
    }

    public OrderResponse submitUtr(String email, String orderId, SubmitUtrRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        if (!order.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You do not own this order.");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot submit UTR for an already PAID order.");
        }

        String cleanUtr = request.getUtr().trim();

        // Check if this UTR is already used by another order
        Optional<Order> duplicate = orderRepository.findAll().stream()
                .filter(o -> cleanUtr.equalsIgnoreCase(o.getUtr()) && !o.getOrderId().equals(orderId))
                .findFirst();

        if (duplicate.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This UTR has already been submitted for another order.");
        }

        order.setUtr(cleanUtr);
        order.setPaymentStatus(PaymentStatus.VERIFICATION_PENDING);
        order.setUpdatedAt(Instant.now());

        Order saved = orderRepository.save(saved = order);
        return mapToOrderResponse(saved);
    }

    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // Admin Endpoints
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getPendingOrders() {
        return orderRepository.findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.VERIFICATION_PENDING).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse approveOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is already marked as PAID.");
        }

        // Generate final Claim Voucher Promo Code
        Voucher voucher = voucherRepository.findById(order.getVoucherId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher reference not found."));

        String cleanProvider = voucher.getProvider().toUpperCase().replaceAll("\\s+", "");
        String randomHex = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String voucherCode = cleanProvider + "-VPRO-50-" + randomHex;

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setVoucherCode(voucherCode);
        order.setUpdatedAt(Instant.now());

        Order saved = orderRepository.save(order);
        return mapToOrderResponse(saved);
    }

    public OrderResponse rejectOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reject an already PAID order.");
        }

        order.setPaymentStatus(PaymentStatus.REJECTED);
        order.setUpdatedAt(Instant.now());

        Order saved = orderRepository.save(order);
        return mapToOrderResponse(saved);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setOrderId(order.getOrderId());
        res.setUserId(order.getUserId());
        res.setVoucherId(order.getVoucherId());
        res.setAmount(order.getAmount());
        res.setUtr(order.getUtr());
        res.setUpiLink(order.getUpiLink());
        res.setPaymentStatus(order.getPaymentStatus().name());
        res.setVoucherCode(order.getVoucherCode());
        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());

        // Dynamic QR code generation (bypasses database storage)
        res.setQrCodeData(QrCodeGenerator.generateQrCodeBase64(order.getUpiLink()));

        // Populate Voucher Details if present
        voucherRepository.findById(order.getVoucherId()).ifPresent(v -> {
            res.setVoucherTitle(v.getTitle());
            res.setVoucherProvider(v.getProvider());
        });

        // Populate User Details if present
        userRepository.findById(order.getUserId()).ifPresent(u -> {
            res.setUserEmail(u.getEmail());
            res.setUserName(u.getName());
        });

        return res;
    }
}
