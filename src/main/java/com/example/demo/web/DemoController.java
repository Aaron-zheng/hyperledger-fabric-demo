package com.example.demo.web;


import com.example.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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

    @RequestMapping("query/{key}")
    @ResponseBody
    public Object query(@PathVariable String key) {
        if(null == key || key.equals("")) {
            key = "center";
        }
        return demoService.query(key);
    }


    @RequestMapping("buyLuckyNumber/{key}/{number}")
    @ResponseBody
    public Object buyLuckyNumber(@PathVariable String key, @PathVariable String number) {
        return demoService.buyLuckyNumber(key, number);
    }

    @RequestMapping("inputLuckyNumber/{number}")
    @ResponseBody
    public Object inputLuckyNumber(@PathVariable String number) {
        return demoService.inputLuckyNumber(number);
    }
}
