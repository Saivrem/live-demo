package biz.icecat.icedatav2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Keeps the retired admin route closed for every HTTP method. */
@RestController
public class AdminController {

    @RequestMapping({"/admin", "/admin/", "/admin/**"})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void disabled() {
    }
}
