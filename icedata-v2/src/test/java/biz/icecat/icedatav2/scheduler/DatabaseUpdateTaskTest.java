package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.DataUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseUpdateTaskTest {

    @Test
    void runsEachSourceOncePerInvocation() {
        DataUpdateService languagesUpdateService = mock(DataUpdateService.class);
        DataUpdateService suppliersUpdateService = mock(DataUpdateService.class);
        new DatabaseUpdateTask(List.of(languagesUpdateService, suppliersUpdateService)).updateDatabase();
        verify(languagesUpdateService).update();
        verify(suppliersUpdateService).update();
        verifyNoMoreInteractions(languagesUpdateService, suppliersUpdateService);
    }

    @Test
    void oneUnavailableSourceDoesNotSkipTheOtherDailyUpdate() {
        DataUpdateService failingUpdateService = mock(DataUpdateService.class);
        DataUpdateService availableUpdateService = mock(DataUpdateService.class);
        when(failingUpdateService.update()).thenThrow(new IllegalStateException("Source unavailable"));
        assertThatCode(() -> new DatabaseUpdateTask(List.of(failingUpdateService, availableUpdateService)).updateDatabase()).doesNotThrowAnyException();
        verify(availableUpdateService).update();
    }

    @Test
    void configuredDailyCronDoesNotDriftWhenKyivChangesClocks() {
        YamlPropertiesFactoryBean yamlProperties = new YamlPropertiesFactoryBean();
        yamlProperties.setResources(new ClassPathResource("application.yml"));
        Properties settings = yamlProperties.getObject();
        CronExpression dailySchedule = CronExpression.parse(settings.getProperty("icedata-v2.cron"));
        ZoneId updateZone = ZoneId.of(settings.getProperty("icedata-v2.update-zone"));
        ZonedDateTime firstExecution = dailySchedule.next(ZonedDateTime.of(2024, 3, 30, 11, 0, 0, 0, updateZone));
        ZonedDateTime secondExecution = dailySchedule.next(firstExecution);
        assertThat(firstExecution.getHour()).isEqualTo(10);
        assertThat(Duration.between(firstExecution, secondExecution)).isEqualTo(Duration.ofDays(1));
    }
}
