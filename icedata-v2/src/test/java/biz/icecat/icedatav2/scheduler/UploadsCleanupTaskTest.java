package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.UploadsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class UploadsCleanupTaskTest {

    @Test
    void shouldCleanupUploadsOncePerInvocation() throws IOException {
        UploadsService uploadsService = mock(UploadsService.class);
        UploadsCleanupTask cleanupTask = new UploadsCleanupTask(uploadsService);

        cleanupTask.cleanupUploads();

        verify(uploadsService).cleanup();
        verifyNoMoreInteractions(uploadsService);
    }

    @Test
    void shouldAllowNextCleanupAfterStorageFailure() throws IOException {
        UploadsService uploadsService = mock(UploadsService.class);
        doThrow(new IOException("Storage unavailable")).doNothing().when(uploadsService).cleanup();
        UploadsCleanupTask cleanupTask = new UploadsCleanupTask(uploadsService);

        assertThatCode(cleanupTask::cleanupUploads).doesNotThrowAnyException();
        cleanupTask.cleanupUploads();

        verify(uploadsService, times(2)).cleanup();
    }

    @Test
    void shouldRunAtMidnightUtcEveryDay() {
        YamlPropertiesFactoryBean yamlProperties = new YamlPropertiesFactoryBean();
        yamlProperties.setResources(new ClassPathResource("application.yml"));
        Properties properties = yamlProperties.getObject();
        ZoneId cleanupZone = ZoneId.of(properties.getProperty("uploads.cleanup-zone"));
        CronExpression cleanupSchedule = CronExpression.parse(properties.getProperty("uploads.cleanup-cron"));
        ZonedDateTime nextExecution = cleanupSchedule.next(ZonedDateTime.of(2024, 3, 30, 23, 59, 59, 0, cleanupZone));

        assertThat(cleanupZone).isEqualTo(ZoneId.of("UTC"));
        assertThat(nextExecution.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(Duration.between(nextExecution, cleanupSchedule.next(nextExecution))).isEqualTo(Duration.ofDays(1));
    }
}
