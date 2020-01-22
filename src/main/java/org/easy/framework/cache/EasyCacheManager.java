package org.easy.framework.cache;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */

//@EqualsAndHashCode(callSuper = true)
@Slf4j
public class EasyCacheManager implements org.springframework.cache.CacheManager {


    private CacheConfiguration<Object, Object> configuration;
    private CacheManager cacheManager;

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

    private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();
    private Set<String> cacheNames;
    private boolean dynamic = true;
    @Resource
    RedisTemplate redisTemplate;

    protected RedisCacheManager createRedisCache(){
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(new FastJsonRedisSerializer<>(Object.class)));
        config.entryTtl(Duration.ofSeconds(1000*10L));
        RedisCacheManager cacheManager = RedisCacheManager.builder(redisTemplate.getConnectionFactory()).cacheDefaults(config).build();

        return cacheManager;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if(cache != null) {
            return cache;
        }
        if(!dynamic && !cacheNames.contains(name)) {
            return cache;
        }

        cache = new EasyCache(name, redisTemplate,getEhcache(name),false);
        Cache oldCache = cacheMap.putIfAbsent(name, cache);
        log.debug("create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    public org.ehcache.CacheManager ehCacheManager(){
        long ehcacheExpire = 1000;//redisEhcacheProperties.getEhcache().getExpireAfterWrite();
        this.configuration =
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Object.class, Object.class,
                                ResourcePoolsBuilder.heap(100))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcacheExpire)))
                        .build();

        cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder()
                .build();
        cacheManager.init();
        return cacheManager;
    }
    public org.ehcache.Cache<Object, Object> getEhcache(String name){
        org.ehcache.Cache<Object, Object> res = ehCacheManager().getCache(name, Object.class, Object.class);
        if(res != null){
            return res;
        }
        return cacheManager.createCache(name, configuration);
    }



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
