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

    @RequestMapping("initial")
    @ResponseBody
    public String initial() {
        return demoService.initial();
    }

    @RequestMapping("startEvent")
    @ResponseBody
    public String startEvent() {
        return demoService.startEvent();
    }

    @RequestMapping("endEvent")
    @ResponseBody
    public String endEvent() {
        return demoService.endEvent();
    }

    @RequestMapping("isEventStarted")
    @ResponseBody
    public String isEventStarted() {
        return demoService.isEventStarted();
    }

}
