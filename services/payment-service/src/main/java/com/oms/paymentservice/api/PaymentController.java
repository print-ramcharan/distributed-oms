package com.oms.paymentservice.api;

import com.oms.paymentservice.domain.Payment;
import com.oms.paymentservice.repository.PaymentRepository;
import com.oms.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<Payment> refundPayment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.refundPayment(orderId));
    }
}
