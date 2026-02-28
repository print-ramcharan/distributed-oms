package com.oms.orderservice.infrastructure.chaos;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    public static volatile boolean killOrderService = false;
    public static volatile boolean latencyEnabled = false;
    public static volatile int maxTps = -1; // -1 = unlimited

    /** GET /chaos/status — read current chaos state (for frontend polling) */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "killOrderService", killOrderService,
                "latencyEnabled", latencyEnabled,
                "maxTps", maxTps);
    }

    /** POST /chaos/kill-service?enable=true|false */
    @PostMapping("/kill-service")
    public String killService(@RequestParam boolean enable) {
        killOrderService = enable;
        return "Order Service Kill Switch: " + enable;
    }

    /** POST /chaos/latency?enable=true|false — inject artificial 3-5s delay */
    @PostMapping("/latency")
    public String toggleLatency(@RequestParam boolean enable) {
        latencyEnabled = enable;
        return "Latency Injection: " + enable;
    }

    /**
     * POST /chaos/throttle?tps=N
     * Set a max TPS cap on order processing (use -1 to remove throttle).
     * Enforced via a RateLimiter in the order command service.
     */
    @PostMapping("/throttle")
    public String setThrottle(@RequestParam int tps) {
        maxTps = tps;
        return "TPS Throttle set to: " + (tps == -1 ? "unlimited" : tps + " req/s");
    }
}
