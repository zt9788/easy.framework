package org.easy.framework.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Slf4j
@Deprecated
public class EasyRedisCache extends RedisCache {
    @Setter
    @Getter
    private Long preloadSecondTime;

    protected EasyRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
    }
    public ValueWrapper get(Object key) {
        ValueWrapper valueWrapper= super.get(key);
//        if(null!=valueWrapper){
//            Long ttl= this.redisOperations.getExpire(key);
//            if(null!=ttl&& ttl<=this.preloadSecondTime){
//                log.info("key:{} ttl:{} preloadSecondTime:{}",key,ttl,preloadSecondTime);
//                ThreadTaskHelper.run(new Runnable() {
//                    @Override
//                    public void run() {
//                        //重新加载数据
//                        logger.info("refresh key:{}",key);
//
//                        CustomizedRedisCache.this.getCacheSupport().refreshCacheByKey(CustomizedRedisCache.super.getName(),key.toString());
//                    }
//                });
//
//            }
//        }
        return valueWrapper;
    }
}
