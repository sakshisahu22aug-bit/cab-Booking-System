package com.labmentix.cabbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/cab_booking_test_db",
        "spring.datasource.driver-class-name=org.postgresql.Driver"
})
class CabBookingApplicationTests {

    @Test
    void contextLoads() {
    }
}
