package com.voucherpro;

import com.voucherpro.dto.CreateOrderRequest;
import com.voucherpro.dto.OrderResponse;
import com.voucherpro.dto.SubmitUtrRequest;
import com.voucherpro.model.User;
import com.voucherpro.model.UserRole;
import com.voucherpro.model.Voucher;
import com.voucherpro.repository.OrderRepository;
import com.voucherpro.repository.UserRepository;
import com.voucherpro.repository.VoucherRepository;
import com.voucherpro.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentModuleIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    private User testUser;
    private Voucher testVoucher;

    @BeforeEach
    public void setUp() {
        orderRepository.deleteAll();
        
        // Clean up test data if present to make tests repeatable
        userRepository.findByEmailIgnoreCase("testuser@voucherpro.com").ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("second@voucherpro.com").ifPresent(userRepository::delete);
        voucherRepository.findById("test-aws").ifPresent(voucherRepository::delete);

        // Create test user
        testUser = new User("testuser@voucherpro.com", "Test User", "password", UserRole.USER);
        userRepository.save(testUser);

        // Create test voucher
        testVoucher = new Voucher();
        testVoucher.setId("test-aws");
        testVoucher.setTitle("AWS Solutions Architect");
        testVoucher.setProvider("AWS");
        testVoucher.setDiscountPrice(75.00);
        testVoucher.setOriginalPrice(150.00);
        testVoucher.setBadge("SAA-C03");
        testVoucher.setRequirements(List.of("Basic knowledge"));
        testVoucher.setIconName("Cloud");
        voucherRepository.save(testVoucher);
    }

    @Test
    public void testCompletePaymentVerificationFlow() {
        // 1. Create Order
        CreateOrderRequest createReq = new CreateOrderRequest(testVoucher.getId());
        OrderResponse orderRes = orderService.createOrder(testUser.getEmail(), createReq);

        assertNotNull(orderRes);
        assertEquals("test-aws", orderRes.getVoucherId());
        assertEquals(75.00, orderRes.getAmount());
        assertEquals("PENDING", orderRes.getPaymentStatus());
        assertNotNull(orderRes.getQrCodeData());
        assertTrue(orderRes.getQrCodeData().startsWith("data:image/png;base64,"));
        assertNull(orderRes.getUtr());
        assertNull(orderRes.getVoucherCode());

        // 2. Submit UTR
        SubmitUtrRequest utrReq = new SubmitUtrRequest("123456789012");
        OrderResponse submittedRes = orderService.submitUtr(testUser.getEmail(), orderRes.getOrderId(), utrReq);

        assertEquals("123456789012", submittedRes.getUtr());
        assertEquals("VERIFICATION_PENDING", submittedRes.getPaymentStatus());

        // 3. Approve Order (Admin Action)
        OrderResponse approvedRes = orderService.approveOrder(orderRes.getOrderId());

        assertEquals("PAID", approvedRes.getPaymentStatus());
        assertNotNull(approvedRes.getVoucherCode());
        assertTrue(approvedRes.getVoucherCode().contains("AWS-VPRO-50-"));

        // 4. Verify duplicate UTR submission prevention
        User secondUser = new User("second@voucherpro.com", "Second User", "password", UserRole.USER);
        userRepository.save(secondUser);
        
        OrderResponse secondOrder = orderService.createOrder(secondUser.getEmail(), createReq);
        
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.submitUtr(secondUser.getEmail(), secondOrder.getOrderId(), utrReq);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("This UTR has already been submitted for another order"));
    }
}
