package com.oms.orderservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.orderservice.api.dto.CreateOrderRequest;
import com.oms.orderservice.api.dto.OrderItemRequest;
import com.oms.orderservice.application.OrderCommandService;
import com.oms.orderservice.domain.model.Order;
import com.oms.orderservice.domain.model.OrderItem;
import com.oms.orderservice.infrastructure.idempotency.IdempotencyResult;
import com.oms.orderservice.infrastructure.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@org.springframework.context.annotation.Import(com.oms.orderservice.config.SecurityConfig.class)
class OrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private OrderCommandService orderCommandService;

        @MockBean
        private IdempotencyService idempotencyService;

        @MockBean
        private com.oms.orderservice.application.OrderQueryService orderQueryService;

        @MockBean
        private com.oms.orderservice.domain.repository.OrderQueryRepository orderQueryRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void shouldCreateOrderEndpoint() throws Exception {

                String idempotencyKey = UUID.randomUUID().toString();
                String userId = UUID.randomUUID().toString();

                OrderItemRequest itemReq = new OrderItemRequest();
                itemReq.setProductId("prod-1");
                itemReq.setQuantity(1);
                itemReq.setPrice(BigDecimal.valueOf(100));

                CreateOrderRequest request = new CreateOrderRequest();
                request.setItems(List.of(itemReq));
                request.setCustomerEmail("test@example.com");
                // request.setUserId(...) is optional now if header is present

                when(idempotencyService.tryAcquire(idempotencyKey))
                                .thenReturn(IdempotencyResult.acquired());

                Order createdOrder = Order.create(
                                List.of(OrderItem.create("prod-1", 1, BigDecimal.valueOf(100))),
                                "test@example.com",
                                UUID.fromString(userId));

                when(orderCommandService.createOrder(any(), any(), any()))
                                .thenReturn(createdOrder);

                mockMvc.perform(post("/orders")
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-User-Id", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("PENDING"));

                verify(idempotencyService).markCompleted(eq(idempotencyKey), any(), any());
        }

        @Test
        void shouldReturnConflictWhenInProgress() throws Exception {

                String idempotencyKey = UUID.randomUUID().toString();
                String userId = UUID.randomUUID().toString();

                when(idempotencyService.tryAcquire(idempotencyKey))
                                .thenReturn(IdempotencyResult.inProgress());

                OrderItemRequest itemReq = new OrderItemRequest();
                itemReq.setProductId("prod-1");
                itemReq.setQuantity(1);
                itemReq.setPrice(BigDecimal.valueOf(100));

                CreateOrderRequest request = new CreateOrderRequest();
                request.setItems(List.of(itemReq));
                request.setCustomerEmail("test@example.com");

                mockMvc.perform(post("/orders")
                                .header("Idempotency-Key", idempotencyKey)
                                .header("X-User-Id", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }
}