package com.erp.pizzeria.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CocinaController {

    @GetMapping("/cocina")
    public String cocina() {
        return "cocina/cocina";
    }
}
