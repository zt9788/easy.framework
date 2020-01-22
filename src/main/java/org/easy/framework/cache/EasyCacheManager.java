package org.easy.framework.cache;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Deprecated
//@EqualsAndHashCode(callSuper = true)
@Slf4j
public class EasyCacheManager extends RedisCacheManager {
    public EasyCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }
    /**
     * 缓存参数的分隔符
     * 数组元素0=缓存的名称
     * 数组元素1=缓存过期时间TTL
     * 数组元素2=缓存在多少秒开始主动失效来强制刷新
     */
    @Getter
    @Setter
    private String separator = "#";

    /**
     * 缓存主动在失效前强制刷新缓存的时间
     * 单位：秒
     */
    @Setter
    @Getter
    private long preloadSecondTime=0;

//    @Override
//    public Cache getCache(String name) {
//
//        String[] cacheParams=name.split(this.getSeparator());
//        String cacheName = cacheParams[0];
//
//        if(StringUtils.isBlank(cacheName)){
//            return null;
//        }
//
//        Long expirationSecondTime = this.computeExpiration(cacheName);
//
//        if(cacheParams.length>1) {
//            expirationSecondTime=Long.parseLong(cacheParams[1]);
//            this.setDefaultExpiration(expirationSecondTime);
//        }
//        if(cacheParams.length>2) {
//            this.setPreloadSecondTime(Long.parseLong(cacheParams[2]));
//        }
//
//        Cache cache = super.getCache(cacheName);
//        if(null==cache){
//            return cache;
//        }
//        log.info("expirationSecondTime:"+expirationSecondTime);
//        EasyRedisCache redisCache= new EasyRedisCache(
//                cacheName,
//                (this.isUsePrefix() ? this.getCachePrefix().prefix(cacheName) : null),
//                this.getRedisOperations(),
//                expirationSecondTime,
//                preloadSecondTime);
//        return redisCache;
//
//    }
}
