package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.aspect.Measured;
import biz.icecat.icedatav2.service.DataUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseUpdateTask {

    private final DataUpdateService supplierDataUpdateService;
    private final DataUpdateService languagesDataUpdateService;

    @Scheduled(cron = "${icedata-v2.cron}")
    public void updateDatabase() {
        updateSuppliers();
        updateLanguages();
    }

    @Measured(methodName = "Update Suppliers info")
    private void updateSuppliers() {
        try {
            int supplierEntriesUpdated = supplierDataUpdateService.update();
            log.info("Updated {} suppliers", supplierEntriesUpdated);
        } catch (Exception e) {
            log.warn("Failed to update suppliers list on");
        }
    }

    @Measured(methodName = "Update languages")
    private void updateLanguages() {
        try {
            int updatedLanguages = languagesDataUpdateService.update();
            log.info("Updated {} languages and", updatedLanguages);
        } catch (Exception e) {
            log.warn("Failed to update languages table on");
        }
    }
}
