package com.oms.paymentservice;

import com.oms.paymentservice.service.PaymentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

//    @Bean
//    CommandLineRunner testPayment(PaymentService paymentService){
//        return args -> {
//            UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
//
//            paymentService.createPendingPayment(orderId, new BigDecimal("499.99"));
//            paymentService.createPendingPayment(orderId, new BigDecimal("499.99"));
//            System.out.println("Payment creation attempted twice for same orderId");
//
//        };
//    }
}

