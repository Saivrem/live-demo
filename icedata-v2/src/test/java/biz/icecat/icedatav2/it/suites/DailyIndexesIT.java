package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseIT;
import biz.icecat.icedatav2.models.entity.DailyIndexEntity;
import biz.icecat.icedatav2.repository.DailyIndexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Sql(statements = "DELETE FROM daily_indexes", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = "DELETE FROM daily_indexes", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DailyIndexesIT extends BaseIT {
    @Autowired
    private DailyIndexRepository repository;

    @Test
    void migratedTablePersistsChecksAndFreeApiExcludesFullRepository() {
        for (String repo : List.of("freexml", "level4")) {
            repository.save(new DailyIndexEntity().setRepository(repo).setLangCode("EN").setFormat("xml")
                    .setCheckedAt(Instant.now()).setLastModified(Instant.parse("2026-10-26T23:59:59Z"))
                    .setHttpStatus(200).setResult("OK"));
        }
        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findByRepositoryAndLangCodeAndFormat("freexml", "EN", "xml"))
                .isPresent().get().extracting(DailyIndexEntity::getLastModified)
                .isEqualTo(Instant.parse("2026-10-26T23:59:59Z"));
        var response = queryEndpointGet("daily-indexes", List.of(), HttpHeaders.EMPTY);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("freexml", "2026-10-26T23:59:59Z", "checkedAt")
                .doesNotContain("level4");
    }
}
