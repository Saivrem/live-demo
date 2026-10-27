package biz.icecat.icedatav2.controller;

import biz.icecat.icedatav2.models.api.ApiDailyIndex;
import biz.icecat.icedatav2.service.impl.DailyIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("daily-indexes")
@RequiredArgsConstructor
public class DailyIndexController {
    private final DailyIndexService service;

    @GetMapping
    public List<ApiDailyIndex> findAll() {
        return service.findAll();
    }
}
