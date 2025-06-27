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
            try {
                service.update();
            } catch (Exception e) {
                log.warn("Couldn't finish update {}", service.getClass().getSimpleName());
            }
        });
    }
}
