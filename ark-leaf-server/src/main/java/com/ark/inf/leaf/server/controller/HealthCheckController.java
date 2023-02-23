package com.ark.inf.leaf.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HealthCheckController {

    @RequestMapping(value = "/tech/health/check", method = RequestMethod.GET)
    @ResponseBody
    public Object healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "");
        return result;
    }
}