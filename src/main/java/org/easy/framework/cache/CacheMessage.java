package org.easy.framework.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: Zhangtong
 * @description:
 * @Date: 2020/1/22.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheMessage implements Serializable,Cloneable {
    private String cacheName;

    private Object key;

    private Integer sender;

    public CacheMessage(String name, Object o) {
        this.cacheName = name;
        this.key = o;
    }
}
