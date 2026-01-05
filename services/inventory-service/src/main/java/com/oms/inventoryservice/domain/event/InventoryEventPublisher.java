package com.oms.inventoryservice.domain.event;

import com.oms.eventcontracts.events.InventoryReservedEvent;
import com.oms.eventcontracts.events.InventoryUnavailableEvent;

public interface InventoryEventPublisher {

    void publishInventoryReserved(InventoryReservedEvent event);

    void publishInventoryUnavailable(InventoryUnavailableEvent event);
}
