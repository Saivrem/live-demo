package biz.icecat.icedatav2.configuration.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "daily-index")
public class DailyIndexProperties {
    public enum Access { FREE, FULL }

    @NotNull
    private Access access = Access.FREE;
    @NotNull
    private Duration delay = Duration.ofHours(3);
    @Min(1)
    private int timeoutMillis = 10000;

    @AssertTrue(message = "daily-index.delay must be positive")
    public boolean isDelayPositive() {
        return delay != null && !delay.isNegative() && !delay.isZero();
    }

    public List<String> repositories() {
        return access == Access.FULL ? List.of("freexml", "level4") : List.of("freexml");
    }
}
