package biz.icecat.icedatav2.models.refs.languages;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LanguageName {
    private Long langNameId;
    private Long translationLangId;
    private Long targetLangId;
    private String nameTranslation;
    private Long updated;
}

