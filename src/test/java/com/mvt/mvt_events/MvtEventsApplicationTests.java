package com.mvt.mvt_events;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MvtEventsApplicationTests {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
        assertTrue(true, "Application context should load successfully");
    }

}
