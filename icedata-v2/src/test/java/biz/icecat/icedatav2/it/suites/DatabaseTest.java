package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

@ActiveProfiles("it")
public class DatabaseTest extends BaseIT {

    @Autowired
    private SupplierRepository repository;

    @Test
    @Sql("/db/init_suppliers.sql")
    void shouldExtractEntriesFromDb() {
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> !repository.findAll().isEmpty());
    }
}
