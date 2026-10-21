package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("it")
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class AdminIT extends BaseIT {

    @Test
    void adminIsClosed() {
        assertEquals(403,
                queryEndpointGet("admin", List.of(), new HttpHeaders())
                        .getStatusCode().value());
    }
}
