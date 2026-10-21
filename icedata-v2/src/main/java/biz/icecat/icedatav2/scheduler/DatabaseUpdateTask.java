package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.DataUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseUpdateTask {

    private final List<DataUpdateService> dataUpdateServices;

    @Scheduled(cron = "${icedata-v2.cron}", zone = "${icedata-v2.update-zone:UTC}")
    public void updateDatabase() {
        for (DataUpdateService dataUpdateService : dataUpdateServices) {
            try {
                dataUpdateService.update();
            } catch (RuntimeException exception) {
                log.error("Scheduled update failed for {}; continuing with other sources",
                        dataUpdateService.getClass().getSimpleName(), exception);
            }
        }
    }
}
