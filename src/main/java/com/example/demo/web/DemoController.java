package com.example.demo.web;


import com.example.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("demo")
public class DemoController {

    @Autowired
    private DemoService demoService;

    @RequestMapping("start")
    @ResponseBody
    public String start() {
        return demoService.start();
    }

    @RequestMapping("query")
    @ResponseBody
    public String query() {
        return demoService.query();
    }

    @RequestMapping("transfer")
    @ResponseBody
    public String transfer() {
        return demoService.transfer();
    }
}
