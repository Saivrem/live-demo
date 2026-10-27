package biz.icecat.icedatav2.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "daily_indexes", uniqueConstraints = @UniqueConstraint(columnNames = {"repository", "lang_code", "format"}))
@Accessors(chain = true)
public class DailyIndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String repository;
    @Column(name = "lang_code")
    private String langCode;
    private String format;
    @Column(name = "last_modified")
    private Instant lastModified;
    @Column(name = "checked_at")
    private Instant checkedAt;
    @Column(name = "http_status")
    private Integer httpStatus;
    private String result;
}
