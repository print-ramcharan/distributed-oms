package com.oms.eventcontracts.commands;

import java.math.BigDecimal;
import java.util.UUID;


public class InitiatePaymentCommand {
    private UUID orderId;
    private BigDecimal amount;

    public InitiatePaymentCommand(){}

    public InitiatePaymentCommand(UUID orderId, BigDecimal amount){
        this.orderId = orderId;
        this.amount = amount;

    }

    public UUID getOrderId(){
        return orderId;
    }
    public BigDecimal getAmount(){
        return amount;
    }

}
