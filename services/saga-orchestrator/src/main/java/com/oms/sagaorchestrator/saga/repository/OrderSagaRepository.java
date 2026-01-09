package com.oms.sagaorchestrator.saga.repository;

import com.oms.sagaorchestrator.saga.domain.OrderSaga;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {

//    Optiona/l<OrderSaga> findById(@Param("id") UUID id);
}
