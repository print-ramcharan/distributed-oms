package com.oms.orderservice.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class OrderCreationSimulation extends Simulation {

    
    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080") 
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    
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

    
    {
        setUp(
                scn.injectOpen(
                        nothingFor(2), 
                        atOnceUsers(10), 
                        rampUsers(50).during(Duration.ofSeconds(10)) 
                )).protocols(httpProtocol);
    }
}
