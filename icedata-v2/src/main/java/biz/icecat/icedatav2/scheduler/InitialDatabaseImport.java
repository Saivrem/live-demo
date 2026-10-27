package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.DataUpdateService;
import biz.icecat.icedatav2.service.impl.DailyIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "initial-start")
@RequiredArgsConstructor
public class InitialDatabaseImport implements ApplicationRunner {

    private final List<DataUpdateService> dataUpdateServices;
    private final DailyIndexService dailyIndexService;

    @Override
    public void run(ApplicationArguments arguments) {
        log.info("Starting initial database import");
        for (DataUpdateService service : dataUpdateServices) {
            int processed = service.update();
            if (processed <= 0) {
                throw new IllegalStateException("Initial import produced no records for "
                        + service.getClass().getSimpleName() + "; check source settings and import logs");
            }
            log.info("Initial import: {} processed {} records", service.getClass().getSimpleName(), processed);
        }
        log.info("Initial database import completed");
        dailyIndexService.refreshIfDue();
    }
}
