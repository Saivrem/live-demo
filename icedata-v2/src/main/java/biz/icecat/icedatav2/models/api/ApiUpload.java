package biz.icecat.icedatav2.models.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class ApiUpload {

    private String name;
    private String url;
    private long size;
}
