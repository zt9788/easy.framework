package org.easy.framework.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.easy.framework.annotation.concurrent.ConcurrentRequset;
import org.easy.framework.annotation.concurrent.ConcurrentRequsetComponent;
import org.easy.framework.lru.EasyLRU;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/21.
 */
@Slf4j
@RestController
@RequestMapping("demo")
@ConcurrentRequsetComponent
public class DemoController {
    @Resource
    EasyLRU<String,String> easyLRU;

    @RequestMapping(value = "test",method = RequestMethod.POST)
    @ResponseBody
    @ConcurrentRequset
    public
    String demoController(@RequestParam(required = true,name = "key") String key) throws InterruptedException {
        String value= "";
        value = easyLRU.get(key);
        if(StringUtils.isEmpty(value)) {
            value = "demoController" + key;
            easyLRU.put(key,value,20*1000);
        }
        Thread.sleep(1000);
        return value;
    }
}
