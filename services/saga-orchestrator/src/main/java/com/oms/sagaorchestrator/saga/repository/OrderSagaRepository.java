package com.oms.sagaorchestrator.saga.repository;

import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {

}
