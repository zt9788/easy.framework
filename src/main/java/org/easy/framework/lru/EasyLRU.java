package org.easy.framework.lru;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/19.
 */
@Slf4j
@Repository
public class EasyLRU<K, V> implements Serializable,Cloneable {
    private int MAX_CACHE_SIZE;
    private final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_SIZE = 10;
    private static final long DEFAULT_TIME_OUT=1000;
    private long timeout;
    private AtomicLong put_count = new AtomicLong();

    LinkedHashMap<K, Entry<V>> map;
    LinkedHashMap<K, Entry<V>> mapOne;
    @Data
    private class Entry<T>{
        private T value;
        private K key;
        private long timeout;
        private long saveTime;
        public Entry(K k, T v, long timeout){
            this.key = k;
            this.value = v;
            this.timeout= timeout;
            this.saveTime = new Date().getTime();
        }
    }
    @SuppressWarnings(value = {"unused"})
    public synchronized void setSize(int size){
        MAX_CACHE_SIZE = size;
        LinkedHashMap<K, Entry<V>> tempMap = new LinkedHashMap<K, Entry<V>>(size, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        tempMap.putAll(map);
        map.clear();
        map = tempMap;
    }
    @SuppressWarnings(value = {"unused"})
    public void setTimeout(long timeout){
        this.timeout = timeout;
    }

    public EasyLRU(){
        this(DEFAULT_SIZE,DEFAULT_TIME_OUT);
    }

    public EasyLRU(int cacheSize,long timeout) {
        MAX_CACHE_SIZE = cacheSize;
        this.timeout = timeout;
        //根据cacheSize和加载因子计算hashmap的capactiy，+1确保当达到cacheSize上限时不会触发hashmap的扩容，
        int capacity = (int) Math.ceil(MAX_CACHE_SIZE / DEFAULT_LOAD_FACTOR) + 1;
        map = new LinkedHashMap<K, Entry<V>>(capacity, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        mapOne = new LinkedHashMap<K, Entry<V>>(capacity*2, DEFAULT_LOAD_FACTOR, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_CACHE_SIZE*2;
            }
        };
    }
    public synchronized void put(K key, V value) {
        this.put(key,value,this.timeout);
    }

    public synchronized void put(K key, V value,long timeout) {
        put_count.getAndIncrement();
        this.getAll();
        if(map.containsKey(key)){
            Entry<V> entry = map.get(key);
            if(value.equals(entry.getValue()))
                return;
            else
                this.remove(key);
        }
        if(mapOne.containsKey(key)) {
            Entry<V> entry = mapOne.get(key);
            if(value.equals(entry.getValue())) {
                map.put(key, new Entry<>(key, value, timeout));
                mapOne.remove(key);
            }else {
                mapOne.put(key, new Entry<>(key,value, timeout));
                map.remove(key);
            }
        }else {
            mapOne.put(key, new Entry<>(key,value, timeout));
        }
    }

    public synchronized V get(K key) {
        Entry<V> entry = map.get(key);
        if(entry != null) {
            V v = entry.getValue();
            long time = entry.getSaveTime();
            long timeout = entry.getTimeout();
            long nowtime = new Date().getTime();
            if((nowtime-time)>timeout) {
                this.remove(key);
                return null;
            }
            return v;
        }
        return null;
    }

    public synchronized void remove(K key) {
        map.remove(key);
        mapOne.remove(key);
    }

    public synchronized Set<Map.Entry<K, V>> getAll() {
        Set<Map.Entry<K,Entry<V>>> set = map.entrySet();
        Set<Map.Entry<K,V>> rset = new CopyOnWriteArraySet<>();
        set.forEach((k)-> rset.add(new LinkedHashMap.SimpleEntry<>(k.getKey(), k.getValue().getValue())));
        rset.forEach((k)->{
            if(this.get(k.getKey()) == null) {
                rset.remove(k);
                this.remove(k.getKey());
            }
        });
        return rset;
    }
    @SuppressWarnings(value = {"unused"})
    public int size() {
        return map.size();
    }

    @SuppressWarnings(value = {"unused"})
    public synchronized void clear() {
        map.clear();
        mapOne.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<K,V> entry : this.getAll()) {
            sb.append(String.format("%s:%s ", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
    @SuppressWarnings(value = {"unused","all"})
    public static void main(String[] args) throws Exception {
        System.out.println("start...");
    }
}
