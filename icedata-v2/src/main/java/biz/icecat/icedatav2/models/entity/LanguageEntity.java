package biz.icecat.icedatav2.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "languages", indexes = {
        @Index(name = "langId", columnList = "lang_id", unique = true)
})
@Accessors(chain = true)
public class LanguageEntity {

    @Id
    @Column(name = "lang_id")
    private Long langId;
    @Column
    private String intLangName;
    @Column
    private String shortCode;
    @Column
    private Long updated;

    @OneToMany(cascade = {CascadeType.ALL})
    @JoinColumn(name = "target_lang_id", referencedColumnName = "lang_id")
    private List<LanguageNameEntity> names;
}
