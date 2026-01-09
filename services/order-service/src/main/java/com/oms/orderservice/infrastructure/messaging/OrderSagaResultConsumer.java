//package com.oms.orderservice.infrastructure.messaging;
//
//
//import com.oms.eventcontracts.events.OrderCompletedEvent;
//import com.oms.eventcontracts.events.OrderFailedEvent;
//import com.oms.orderservice.domain.model.Order;
//import com.oms.orderservice.domain.repository.OrderRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class OrderSagaResultConsumer {
//
//
//        private final OrderRepository orderRepository;
//
//        @KafkaListener(topics = "order.completed", groupId = "order-service")
//        @Transactional
//        public void onCompleted(OrderCompletedEvent event) {
//            log.info("OrderCompletedEvent received: {}", event.getOrderId());
//
//            Order order = orderRepository.findById(event.getOrderId())
//                    .orElseThrow();
//
//            order.markCompleted();
//            orderRepository.save(order);
//        }
//
//        @KafkaListener(topics = "order.failed", groupId = "order-service")
//        @Transactional
//        public void onFailed(OrderFailedEvent event) {
//            log.info("OrderFailedEvent received: {}", event.getOrderId());
//
//            Order order = orderRepository.findById(event.getOrderId())
//                    .orElseThrow();
//
//            order.markFailed(event.getReason());
//            orderRepository.save(order);
//        }
//    }
//
//
