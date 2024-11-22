package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.utils.models.TestOptions;
import biz.icecat.icedatav2.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static biz.icecat.icedatav2.utils.TestConstants.SUPPLIERS_ENDPOINT;

@ActiveProfiles("it")
@Sql(value = "/db/init_suppliers.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class SuppliersIT extends BaseIT {

    @Autowired
    private SupplierRepository repository;

    // TODO Mode somewhere else
    @Test
    void shouldExtractEntriesFromDb() {
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> !repository.findAll().isEmpty());
    }

    @Test
    void shouldFindSuppliers_onGet() {
        verifyGetFlow(TestOptions.of(SUPPLIERS_ENDPOINT,
                200,
                "data/it/supplier/get/expected.json",
                null,
                null));
    }

    @Test
    void shouldCreateSupplier_onValidInput() {
        verifyPostFlow(TestOptions.of(
                SUPPLIERS_ENDPOINT,
                200,
                "data/it/supplier/create/expected.json",
                null,
                "data/it/supplier/create/requestBody.json"
        ));
    }
}
