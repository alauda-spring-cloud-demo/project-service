package demo.ms.project.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/dummy")
@RestController
public class DummyController {

    @HystrixCommand(commandKey = "DummyNumber")
    @GetMapping("/number")
    public int number(int a,int b){
        return a/b;
    }

    @HystrixCommand(commandKey = "DummyTimeout")
    @GetMapping("/timeout")
    public String timeout() throws InterruptedException {
        Thread.sleep(2000);
        return "OK";
    }

    @HystrixCommand(commandKey = "DummyHello",fallbackMethod = "helloFallback")
    @GetMapping("/hello")
    public String hello(String name){
        return "你好:" + name;
    }

    public String helloFallback(String name){
        return "你好:陌生人";
    }
}
