package biz.icecat.icedatav2.base;

import biz.icecat.icedatav2.utils.SaxUtils;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseSaxTest {

    @AfterEach
    void releaseParser() {
        SaxUtils.removeParser();
    }

    protected ByteArrayInputStream xmlStream(String document) {
        return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
    }
}
