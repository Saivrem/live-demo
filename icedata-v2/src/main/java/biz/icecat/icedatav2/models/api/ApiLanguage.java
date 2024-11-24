package biz.icecat.icedatav2.models.api;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ApiLanguage {

    private Long langId;
    private String intLangName;
    private String shortCode;
    private String updated;

    private List<ApiLanguageName> languageNames;
}
