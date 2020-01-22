package org.easy.framework.cache;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.easy.framework.lru.EasyLRU;
import org.omg.CORBA.OBJ_ADAPTER;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.cache.Cache;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: Zhangtong
 * @description: https://blog.csdn.net/byamao1/article/details/82014062
 * @Date: 2020/1/22.
 */

@Slf4j
@Component
public class EasyCache extends AbstractValueAdaptingCache {
    @Getter
    @Setter
    private String name = "easy_cache";

    private RedisTemplate<Object, Object> redisTemplate;

    private org.ehcache.Cache<Object, Object> ehcacheCache;

    private EasyLRU<Object, Object> easyLRU;


    @Setter
    @Getter
    private String cachePrefix ="cachePrefix";
    @Setter
    @Getter
    private long defaultExpiration = 0;
    @Setter
    @Getter
    private Map<String, Long> expires = new HashMap<>();
    @Setter
    @Getter
    private String topic = "cache:redis:ehcache:topic";
    public EasyCache(){
        super(false);
    }

    protected EasyCache(boolean allowNullValues) {
        super(allowNullValues);
//        Caffeine.newBuilder()
//                .expireAfterWrite(1, TimeUnit.SECONDS)
//                .expireAfterAccess(1,TimeUnit.SECONDS)
//                .maximumSize(10)
//                .build();
    }

    public EasyCache(String name, RedisTemplate<Object, Object> redisTemplate,
                     org.ehcache.Cache<Object,Object> ehcacheCache,
                     boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.ehcacheCache = ehcacheCache;
//        this.cachePrefix = redisEhcacheProperties.getCachePrefix();
//        this.defaultExpiration = redisEhcacheProperties.getRedis().getDefaultExpiration();
//        this.expires = redisEhcacheProperties.getRedis().getExpires();
//        this.topic = redisEhcacheProperties.getRedis().getTopic();
    }


    @Override
    protected Object lookup(Object key) {
        Object cacheKey = getKey(key);
        Object value = ehcacheCache.get(key);
        if(value != null) {
            log.debug("get cache from ehcache, the key is : {}", cacheKey);
            return value;
        }

        value = redisTemplate.opsForValue().get(cacheKey);

        if(value != null) {
            log.debug("get cache from redis and put in ehcache, the key is : {}", cacheKey);
            //将二级缓存重新复制到一级缓存。原理是最近访问的key很可能再次被访问
            ehcacheCache.put(key, value);
        }
        return value;

    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public <T> T get(Object key, Callable<T> callable) {
        Object value = lookup(key);
        if(value != null) {
            return (T) value;
        }
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            value = lookup(key);
            if(value != null) {
                return (T) value;
            }
            value = callable.call();
            Object storeValue = toStoreValue(callable.call());
            put(key, storeValue);
            return (T) value;
        } catch (Exception e) {
            try {
                Class<?> c = Class.forName("org.springframework.cache.Cache$ValueRetrievalException");
                Constructor<?> constructor = c.getConstructor(Object.class, Callable.class, Throwable.class);
                RuntimeException exception = (RuntimeException) constructor.newInstance(key, callable, e.getCause());
                throw exception;
            } catch (Exception e1) {
                throw new IllegalStateException(e1);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            this.evict(key);
            return;
        }
        long expire = getExpire();
        if(expire > 0) {
            redisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire, TimeUnit.MILLISECONDS);
        } else {
            redisTemplate.opsForValue().set(getKey(key), toStoreValue(value));
        }
        //通过redis推送消息，使其他服务的ehcache失效。
        //原来的有个缺点：服务1给缓存put完KV后推送给redis的消息，服务1本身也会接收到该消息，
        // 然后会将刚刚put的KV删除。这里把ehcacheCache的hashcode传过去，避免这个问题。
        push(new CacheMessage(this.name, key, this.ehcacheCache.hashCode()));

        ehcacheCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
// 先清除redis中缓存数据，然后清除ehcache中的缓存，避免短时间内如果先清除ehcache缓存后其他请求会再从redis里加载到ehcache中
        redisTemplate.delete(getKey(key));

        push(new CacheMessage(this.name, key, this.ehcacheCache.hashCode()));

        ehcacheCache.remove(key);

    }

    //TODO  算法实现有问题
    @Deprecated
    @Override
    public void clear() {
// 先清除redis中缓存数据，然后清除ehcache中的缓存，避免短时间内如果先清除ehcache缓存后其他请求会再从redis里加载到ehcache中
        Set<Object> keys = redisTemplate.keys(this.name.concat(":"));
        for(Object key : keys) {
            redisTemplate.delete(key);
        }
        push(new CacheMessage(this.name, null));

        ehcacheCache.clear();

    }
    /**
     * @description 清理本地缓存
     * @param key
     */
    public void clearLocal(Object key) {
        log.debug("clear local cache, the key is : {}", key);
        if(key == null) {
            ehcacheCache.clear();
        } else {
            ehcacheCache.remove(key);
        }
    }

    public org.ehcache.Cache<Object, Object> getLocalCache(){
        return ehcacheCache;
    }

    //key的生成    name:cachePrefix:key
    private Object getKey(Object key) {
        return this.name.concat(":").concat(StringUtils.isEmpty(cachePrefix) ? key.toString() : cachePrefix.concat(":").concat(key.toString()));
    }

    private long getExpire() {
        long expire = defaultExpiration;
        Long cacheNameExpire = expires.get(this.name);
        return cacheNameExpire == null ? expire : cacheNameExpire.longValue();
    }
    /**
     * 缓存变更时，利用redis的消息订阅功能，通知其他节点清理本地缓存。
     * @description
     * @param message
     */
    private void push(CacheMessage message) {
        redisTemplate.convertAndSend(topic, message);
    }


}
