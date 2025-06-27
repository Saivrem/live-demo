package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.utils.models.TestOptions;
import biz.icecat.icedatav2.utils.models.UrlParam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@ActiveProfiles("it")
@Sql(value = "/db/init_languages.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class LanguagesIT extends BaseIT {

    @Autowired
    private LanguagesRepository repository;

    @Test
    void shouldExtractEntriesFromDb() {
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> !repository.findAll().isEmpty());
    }

    @Test
    void shouldFindLanguages_onGet() {
        verifyGetFlow(TestOptions.of("languages",
                200,
                "data/it/language/get/expected.json",
                null,
                null));
    }

    @Test
    void shouldFindLanguageById_onGet() {
        verifyGetFlow(TestOptions.of("languages",
                200,
                "data/it/language/get-by-id/expected.json",
                Collections.singletonList(new UrlParam("id", "1")),
                null));
    }
}