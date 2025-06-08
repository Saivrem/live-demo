package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.utils.models.TestOptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles("it")
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class AdminIT extends BaseIT {

    @Test
    void shouldInitializeDatabase_onGet() {
        verifyGetFlow(TestOptions.of("admin",
                200,
                "data/it/admin/get/expected.json",
                null,
                null));
    }
}