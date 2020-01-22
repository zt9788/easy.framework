package org.easy.framework.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */

@Slf4j
@Component
public class CacheMessageListener implements MessageListener {
    @Resource
    private RedisTemplate<Object, Object> redisTemplate;
    @Resource
    private EasyCacheManager easyCacheManager;
    @Override
    public void onMessage(Message message, byte[] bytes) {
        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        log.debug("recevice a redis topic message, clear local cache, the cacheName is {}, the key is {}", cacheMessage.getCacheName(), cacheMessage.getKey());
        easyCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey(), cacheMessage.getSender());

    }
}
