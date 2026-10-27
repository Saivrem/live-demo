package biz.icecat.icedatav2.models.api;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
public class ApiDailyIndex {
    private String repository;
    private String langCode;
    private String format;
    private Instant lastModified;
    private Instant checkedAt;
    private Integer httpStatus;
    private String result;
    private String freshness;
    private boolean checkOverdue;
}
