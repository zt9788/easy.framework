package org.easy.framework.annotation.concurrent;

import java.lang.annotation.*;

/**
 * by zhangtong
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConcurrentRequset {
    //TODO 默认同时可以访问N次（未实现）
    int mutilCount() default 1;
    //是否只检查mehod中包含的request
    boolean onlyRequestMethod() default true;
    //只检查request数组
    String[] requestParams() default {};
    //只检查cookie数组
    String[] cookie() default {};
    //是否同域名
    boolean onlySameDomainCookie() default true;
    //检查session内容，默认不检查
    String[] session() default {};
    //发生错误后的返回值
    String returnObject() default "";
    //默认锁，超时时间
    int timeOut() default 10;

    boolean timeOutForLock() default false;
}
