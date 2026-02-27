package com.oms.notificationservice.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@distributed-oms.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderConfirmation(String to, String orderId, String amount) {
        try {
            log.info("Sending Order Confirmation to: {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@distributed-oms.com");
            message.setTo(to);
            message.setSubject("Order Confirmation: " + orderId);
            message.setText("Thank you for your order!\n\nOrder ID: " + orderId + "\nTotal Amount: " + amount
                    + "\n\nWe will notify you when it ships.");

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            
            
            
        }
    }
}
