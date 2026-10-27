package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.impl.DailyIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DailyIndexUpdateTask {
    private final DailyIndexService service;

    @Scheduled(fixedDelayString = "${daily-index.delay:PT3H}", initialDelayString = "${daily-index.initial-delay:PT1M}")
    public void update() {
        service.refreshIfDue();
    }
}
