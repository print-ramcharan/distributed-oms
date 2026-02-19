package com.oms.orderservice.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class OrderCreationSimulation extends Simulation {

    // 1. HTTP Protocol Configuration
    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080") // Gateway URL
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // 2. Scenario Definition
    ScenarioBuilder scn = scenario("Order Creation Load Test")
            .exec(
                    http("Create Order")
                            .post("/api/orders")
                            .body(StringBody(
                                    """
                                            {
                                              "customerEmail": "loadtest@example.com",
                                              "items": [
                                                { "productId": "PROD-001", "quantity": 1, "price": 50.00 },
                                                { "productId": "PROD-002", "quantity": 2, "price": 25.00 }
                                              ]
                                            }
                                            """))
                            .asJson()
                            .check(status().is(200)));

    // 3. Load Injection Profile
    {
        setUp(
                scn.injectOpen(
                        nothingFor(2), // warmup
                        atOnceUsers(10), // burst
                        rampUsers(50).during(Duration.ofSeconds(10)) // ramp up
                )).protocols(httpProtocol);
    }
}
