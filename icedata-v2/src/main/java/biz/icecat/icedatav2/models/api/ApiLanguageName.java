package biz.icecat.icedatav2.models.api;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ApiLanguageName {
    private Long langNameId;
    private Long translationLangId;
    private Long targetLangId;
    private String nameTranslation;
    private String updated;
}
