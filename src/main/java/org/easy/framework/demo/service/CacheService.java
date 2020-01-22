package org.easy.framework.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Slf4j
@Service
public class CacheService {
    @Cacheable(value = "aaaaaaaaaa",keyGenerator = "keyGenerator")
    public String haha(){
        log.info("hehehehehe");
        return "1111";
    }
}
