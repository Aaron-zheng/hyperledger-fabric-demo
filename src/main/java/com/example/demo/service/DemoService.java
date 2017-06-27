package com.example.demo.service;


public interface DemoService {

    String initial();

    String startEvent();

    String endEvent();

    String isEventStarted();

    String query(String key);

    String buyLuckyNumber(String key, String number);

    String inputLuckyNumber(String number);
}
