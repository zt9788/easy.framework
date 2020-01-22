package org.easy.framework.cache;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.easy.framework.lru.EasyLRU;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.EvictionAdvisor;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
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
 *               https://blog.csdn.net/byamao1/article/details/82014062
 *               https://blog.csdn.net/Gentlemike/article/details/80403967
 * @Date: 2020/1/22.
 */

//@EqualsAndHashCode(callSuper = true)
@Slf4j
@AutoConfigureAfter(RedisAutoConfiguration.class)
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
    //TODO 没配置，这个有点问题
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
        //RedisCache rc = (RedisCache) cacheManager.getCache("");
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
        /*
        ResourcePoolsBuilder
                        .newResourcePoolsBuilder()
                        //设置缓存堆容纳元素个数(JVM内存空间)超出个数后会存到offheap中
                        .heap(1000L,EntryUnit.ENTRIES)
                        //设置堆外储存大小(内存存储) 超出offheap的大小会淘汰规则被淘汰
                        .offheap(100L, MemoryUnit.MB)
                        // 配置磁盘持久化储存(硬盘存储)用来持久化到磁盘,这里设置为false不启用
                        .disk(500L, MemoryUnit.MB, false)
         */
        long ehcacheExpire = 1000;//redisEhcacheProperties.getEhcache().getExpireAfterWrite();
        this.configuration =
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(Object.class, Object.class,
                                ResourcePoolsBuilder.heap(100))

                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcacheExpire)))
//                        .withEvictionAdvisor(new LRUEvictionAdvisor())
                        .build();
        cacheManager = CacheManagerBuilder
                .newCacheManagerBuilder()
                .withCache("a",configuration)
                .build();
        cacheManager.init();
        return cacheManager;
    }

//    EasyLRU<Object,Object> lru = new EasyLRU<>();

    private class LRUEvictionAdvisor implements EvictionAdvisor<Object, Object> {

        @Override
        public boolean adviseAgainstEviction(Object o, Object o2) {
            return false;
        }
    }
    public org.ehcache.Cache<Object, Object> getEhcache(String name){
        org.ehcache.Cache<Object, Object> res = ehCacheManager().getCache(name, Object.class, Object.class);
        if(res != null){
            return res;
        }
        return cacheManager.createCache(name, configuration);
    }

    public void clearLocal(String cacheName, Object key, Integer sender) {
        Cache cache = cacheMap.get(cacheName);
        if(cache == null) {
            return ;
        }
        EasyCache redisEhcacheCache = (EasyCache) cache;
        //如果是发送者本身发送的消息，就不进行key的清除
        if(redisEhcacheCache.getLocalCache().hashCode() != sender) {
            redisEhcacheCache.clearLocal(key);
        }
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
