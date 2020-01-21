package org.easy.framework.demo;

import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.easy.framework.demo.util.HttpClientUtil;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class EasyframeworkApplicationTests {

    @Resource
    HttpClientUtil<String> util;
    @Test
    void contextLoads() throws IOException, URISyntaxException, InterruptedException {
        log.info("start");
        for(int i=0;i<10;i++){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    test();
                }
            });
            thread.start();
        }
        Thread.sleep(10000);
    }
    public void test() {
        Map<String,String> map = new HashMap<>();
        map.put("key","value");
        String ret = null;
        try {
            ret = util.postHttpResponse("http://localhost:8088/demo/test",map,false,new TypeReference<String>(){});
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("{}",ret);
    }

}
