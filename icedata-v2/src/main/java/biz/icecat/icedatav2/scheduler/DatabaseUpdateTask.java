package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.DataUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseUpdateTask {

    private final List<DataUpdateService> services;

    @SneakyThrows
    @Scheduled(cron = "${icedata-v2.cron}")
    public void updateDatabase() {
        services.forEach(service -> {
            String serviceName = service.getClass().getSimpleName();
            try {
                int updated = service.update();
                log.info("{} Updated {} entries", service, updated);
            } catch (Exception e) {
                log.warn("Couldn't finish update {}", serviceName);
            }
        });
    }

/*    @Measured(methodName = "Update Suppliers info")
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
    }*/
}
