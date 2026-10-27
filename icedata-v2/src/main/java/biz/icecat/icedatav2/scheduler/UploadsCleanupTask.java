package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.UploadsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadsCleanupTask {

    private final UploadsService uploadsService;

    @Scheduled(cron = "${uploads.cleanup-cron:0 0 0 * * *}", zone = "${uploads.cleanup-zone:UTC}")
    public void cleanupUploads() {
        try {
            uploadsService.cleanup();
            log.info("Scheduled upload cleanup completed");
        } catch (IOException | RuntimeException exception) {
            log.error("Scheduled upload cleanup failed", exception);
        }
    }
}
