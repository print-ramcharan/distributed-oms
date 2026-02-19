package com.oms.orderservice.infrastructure.chaos;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    public static boolean killOrderService = false;
    public static boolean latencyEnabled = false;

    @PostMapping("/kill-service")
    public String killService(@RequestParam boolean enable) {
        killOrderService = enable;
        return "Order Service Kill Switch: " + enable;
    }

    @PostMapping("/latency")
    public String toggleLatency(@RequestParam boolean enable) {
        latencyEnabled = enable;
        return "Latency Injection: " + enable;
    }
}
