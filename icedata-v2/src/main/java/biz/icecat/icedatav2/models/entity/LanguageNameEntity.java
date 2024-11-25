package biz.icecat.icedatav2.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "language_names")
@Accessors(chain = true)
public class LanguageNameEntity {

    @Id
    @Column(name = "lang_name_id")
    private Long langNameId;
    @Column(name = "translation_lang_id")
    private Long translationLangId;
    @Column(name = "target_lang_id")
    private Long targetLangId;
    @Column(name = "name_translation")
    private String nameTranslation;
    @Column(name = "updated")
    private Long updated;
}