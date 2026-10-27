package biz.icecat.icedatav2.scheduler;

import biz.icecat.icedatav2.service.DataUpdateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InitialDatabaseImportTest {

    private final DataUpdateService languages = mock(DataUpdateService.class);
    private final DataUpdateService suppliers = mock(DataUpdateService.class);
    private final InitialDatabaseImport runner = new InitialDatabaseImport(List.of(languages, suppliers));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("icedata-v2.batch-size=1000")
            .withBean("languages", DataUpdateService.class, () -> languages)
            .withBean("suppliers", DataUpdateService.class, () -> suppliers)
            .withUserConfiguration(InitialDatabaseImport.class);

    @Test
    void normalStartupDoesNotImport() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(InitialDatabaseImport.class));
        verifyNoInteractions(languages, suppliers);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--initial-start", "--initial-start=true"})
    void initialStartImportsBothSources(String option) {
        when(languages.update()).thenReturn(2);
        when(suppliers.update()).thenReturn(3);

        contextRunner.withInitializer(context -> context.getEnvironment().getPropertySources()
                .addFirst(new SimpleCommandLinePropertySource(option)))
                .run(context -> {
                    assertThat(context).hasSingleBean(InitialDatabaseImport.class);
                    context.getBean(InitialDatabaseImport.class).run(new DefaultApplicationArguments(option));
                });

        verify(languages).update();
        verify(suppliers).update();
    }

    @Test
    void explicitFalseDoesNotImport() {
        contextRunner.withPropertyValues("initial-start=false")
                .run(context -> assertThat(context).doesNotHaveBean(InitialDatabaseImport.class));
        verifyNoInteractions(languages, suppliers);
    }

    @Test
    void failedSourceFailsStartup() {
        when(languages.update()).thenThrow(new IllegalStateException("Source unavailable"));
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments("--initial-start")))
                .isInstanceOf(IllegalStateException.class).hasMessage("Source unavailable");
        verifyNoInteractions(suppliers);
    }

    @Test
    void zeroRecordsFailsStartupInsteadOfReportingSuccessfulInitialization() {
        when(languages.update()).thenReturn(0);
        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments("--initial-start")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no records");
        verifyNoInteractions(suppliers);
    }
}
