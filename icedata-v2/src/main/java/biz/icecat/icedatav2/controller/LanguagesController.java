package biz.icecat.icedatav2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("languages")
public class LanguagesController {

    // TODO extract somewhere
    private final static HttpHeaders headers;

    static {
        headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
    }
}
