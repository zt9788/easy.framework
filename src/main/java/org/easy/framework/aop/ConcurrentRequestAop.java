package org.easy.framework.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.easy.framework.annotation.concurrent.ConcurrentRequset;
import org.easy.framework.exception.EasyException;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * by zhangtong
 */
@Aspect
@Order(0x1000000)
@Slf4j
@Component
public class ConcurrentRequestAop {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    RedisTemplate<String,Object> redisTemplate;

    @Pointcut("@within(org.easy.framework.annotation.concurrent.ConcurrentRequsetComponent)")
    public void executeService(){
    }

    @Before("executeService()")
    public void doBeforeAdvice(JoinPoint joinPoint){

    }
    @Around("@within(org.easy.framework.annotation.concurrent.ConcurrentRequsetComponent)")
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint) throws Exception {
        Object obj = null;
        Object[] args = joinPoint.getArgs();
        //获取连接点的方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        //连接点类型
        String kind = joinPoint.getKind();
        //返回连接点方法所在类文件中的位置  打印报异常
        SourceLocation sourceLocation = joinPoint.getSourceLocation();
        ///返回连接点静态部分
        JoinPoint.StaticPart staticPart = joinPoint.getStaticPart();
        //attributes可以获取request信息 session信息等
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        ConcurrentRequset concurrentRequset = signature.getMethod().getAnnotation(ConcurrentRequset.class);
        if(concurrentRequset == null){
            try {
                obj = joinPoint.proceed();
                return obj;
            } catch (Throwable throwable) {
                log.error("runtime error:",throwable);
                recordRuntime(request);
                throw new RuntimeException(throwable);
            }
        }
        StringBuffer sb  = new StringBuffer();
        sb.append(signature.toLongString().replace(" ","+"));
        sb.append("_");
        Arrays.stream(args).forEach(arg->{  //
            try {
                sb.append(OBJECT_MAPPER.writeValueAsString(arg));
                sb.append(";");
            } catch (JsonProcessingException e) {
                log.debug(arg.toString());
            }
        });

//        sb.append(this.readMehod(method));
        sb.append(this.readSession(request, concurrentRequset));
        sb.append(this.readRequestFalse(request, concurrentRequset));
        sb.append(this.readCookie(request, concurrentRequset));
        String lockKey = sb.toString();
//        log.info("-----------");
        log.debug("mutil lock key:{}",lockKey);
        String requestUuid = (String)request.getAttribute("request_uuid");
//
        String value = String.valueOf(System.currentTimeMillis() + concurrentRequset.timeOut()*1000);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey,value);
        Boolean ret = false;
        if(success!=null && success){
            log.debug("【"+requestUuid+"】"+Thread.currentThread() + "加锁成功1！");
            redisTemplate.expire(lockKey, concurrentRequset.timeOut(), TimeUnit.SECONDS);
            log.debug("【"+requestUuid+"】"+Thread.currentThread() + "执行业务逻辑1！");
            ret = true;
        }else{
            //expire失效的状态下，判断存储的时间，来删除可能失效的Key
            String lockValueA = redisTemplate.opsForValue().get(lockKey).toString();
            if (lockValueA != null && Long.parseLong(lockValueA) > (System.currentTimeMillis()+ concurrentRequset.timeOut()*1000)) {
                String lockValueB = redisTemplate.opsForValue().getAndSet(lockKey, String.valueOf(System.currentTimeMillis() + concurrentRequset.timeOut()*1000)).toString();
                if (lockValueB == null || lockValueB.equals(lockValueA)) {
                    log.debug("【"+requestUuid+"】"+Thread.currentThread() + "加锁成功2！");
                    redisTemplate.expire(lockKey, concurrentRequset.timeOut(),TimeUnit.SECONDS);
                    log.debug(Thread.currentThread() + "执行业务逻辑2！");
                    ret = true;
                }else{
                    log.debug("【"+requestUuid+"】方法有锁，直接跳过 {}",request.getRequestURI());
                }
            }
        }
        if(ret) {
            try {
                obj = joinPoint.proceed();
                Object redisObj = redisTemplate.opsForValue().get(lockKey);
                if(redisObj != null){
                    log.info("【"+requestUuid+"】删除锁");
                    redisTemplate.delete(lockKey);
                }
                return obj;
            } catch (Throwable throwable) {
                log.error("runtime error:",throwable);
                recordRuntime(request);
//                return new JsonResult<Object>(EnumJsonResult.SERVICE_ERR, null);
                throw new RuntimeException(throwable);
            }
        }else {
            recordRuntime(request);
            if (StringUtils.isEmpty(concurrentRequset.returnObject())) {
                throw new EasyException("您访问过于频繁");
//                return new JsonResult<Object>(EnumJsonResult.MUTIL_CONNECT_ERR, null);
            }else
                return concurrentRequset.returnObject();
        }
    }
    public void recordRuntime(HttpServletRequest request){
        String ip = getIpAddr(request);
        Object obj = request.getAttribute("request_time");
        if(obj != null) {
            Long datetime = (Long) obj;
            String requestUuid = (String) request.getAttribute("request_uuid");
            String url = request.getRequestURI();
            long l = System.currentTimeMillis() - datetime;
//        log.info("["+ DateUtil.formatDateTime(new Date())+"]["+ip+"]【执行时间 :"+l+"】【"+url+"】"+"#request_uuid:"+ requestUuid +">>>>--");
        }
    }

    @Deprecated
    public void doAfter(JoinPoint joinPoint){
        Object obj = null;
        Object[] args = joinPoint.getArgs();
        //获取连接点的方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ///返回连接点静态部分
        JoinPoint.StaticPart staticPart = joinPoint.getStaticPart();

        ConcurrentRequset concurrentRequset = signature.getMethod().getAnnotation(ConcurrentRequset.class);
        StringBuffer sb  = new StringBuffer();
        sb.append(signature.toLongString().replace(" ","+"));
        sb.append("_");
        Arrays.stream(args).forEach(arg->{  //
            try {
                sb.append(OBJECT_MAPPER.writeValueAsString(arg));
                sb.append(";");
            } catch (JsonProcessingException e) {
                log.debug(arg.toString());
            }
        });
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        log.debug(request.getRequestURL().toString()); //http://127.0.0.1:8080/hello/getName
        log.debug(request.getRemoteAddr()); //127.0.0.1
        log.debug(request.getRequestURI());
        log.debug(request.getMethod()); //GET
//        sb.append(this.readMehod(method));
        sb.append(this.readSession(request, concurrentRequset));
//        sb.append(this.readRequestMethod(method,request,mutilRequset));
        sb.append(this.readRequestFalse(request, concurrentRequset));
        sb.append(this.readCookie(request, concurrentRequset));
        String lockKey = sb.toString();
//        log.info("-----------");
        log.debug("lock key return:{}",lockKey);
        Object success = redisTemplate.opsForValue().get(lockKey);
        String requestUuid = (String)request.getAttribute("request_uuid");
        if(success != null){
            log.info("【"+requestUuid+"】删除锁");
            redisTemplate.delete(lockKey);
        }
    }

    private StringBuffer readRequestMethod(HandlerMethod method, HttpServletRequest request, ConcurrentRequset concurrentRequset){
        if(!concurrentRequset.onlyRequestMethod()) return new StringBuffer();
        StringBuffer sb = new StringBuffer();
        MethodParameter[] methodParameters = method.getMethodParameters();
        for(MethodParameter methodParameter:methodParameters){
            String value = request.getParameter(methodParameter.getParameter().getName());
            sb.append(methodParameter.getParameter().getName());
            sb.append(":");
            sb.append(value);
            sb.append("_");
        }
        return sb;
    }

    private StringBuffer readRequestFalse(HttpServletRequest request, ConcurrentRequset concurrentRequset){
        if(concurrentRequset.onlyRequestMethod()) return new StringBuffer();
        StringBuffer sb = new StringBuffer();
        Map<String, String[]> map =  request.getParameterMap();
        map.forEach((key, value) -> {
            log.debug("Key: " + key + ", Value: " + value);
        });
        for(Map.Entry<String, String[]> entry : map.entrySet()){
            String mapKey = entry.getKey();
            String[] mapValue = entry.getValue();
            if(concurrentRequset.requestParams() == null || concurrentRequset.requestParams().length == 0){
                sb.append(mapKey);
                sb.append(":");
                for(String value:mapValue){
                    sb.append(value);
                    sb.append(",");
                }
            }else{
                String[] params = concurrentRequset.requestParams();
                for(String param: params){
                    if(param.equals(mapKey)){
                        sb.append(mapKey);
                        sb.append(":");
                        for(String value:mapValue){
                            sb.append(value);
                            sb.append(",");
                        }
                    }
                }
            }
            sb.append("-");
        }
        return sb;
    }
    private StringBuffer readSession(HttpServletRequest request, ConcurrentRequset concurrentRequset){
        StringBuffer sb = new StringBuffer();
        if(concurrentRequset.session() == null || concurrentRequset.session().length <=0)
            return sb;
        HttpSession session = request.getSession();
        Enumeration<String> names =  session.getAttributeNames();
        do{
            String name = names.nextElement();
            if(StringUtils.isEmpty(name))
                break;
            for(String value: concurrentRequset.session()){
                if(value.equals(name)){
                    sb.append(name);
                    sb.append(":");
                    sb.append(session.getAttribute(name));
                    sb.append("_");
                }
            }
        }while (names.hasMoreElements());
        return sb;
    }
    private StringBuffer readMehod(HandlerMethod method){
        StringBuffer sb = new StringBuffer();
        sb.append(method.getMethod().getName());
        sb.append(":");
        MethodParameter[] methodParameters = method.getMethodParameters();
        for(MethodParameter methodParameter:methodParameters){
            sb.append(methodParameter.getParameter().getType());
            sb.append(":");
            sb.append(methodParameter.getParameter().getName());
        }
        sb.append("-");
        return sb;
    }
    private StringBuffer readCookie(HttpServletRequest request, ConcurrentRequset concurrentRequset){
        StringBuffer sb = new StringBuffer();
        String url = request.getRequestURL().toString();
        Cookie[] cookies = request.getCookies();
        if(cookies == null) return sb;
        for(Cookie cookie:cookies){
            if(concurrentRequset.onlySameDomainCookie()){
                String domain = cookie.getDomain();
                if(StringUtils.isEmpty(domain))
                    continue;
                if(!url.contains(domain))
                    continue;
            }
            if(concurrentRequset.cookie() != null  && concurrentRequset.cookie().length > 0){
                String[] cookies2 = concurrentRequset.cookie();
                for (String cookieStr : cookies2) {
                    if (cookie.getName().equals(cookieStr)) {
                        sb.append(cookie.getName());
                        sb.append(":");
                        sb.append(cookie.getValue());
                        sb.append("_");
                    }
                }
            }else{
                sb.append(cookie.getName());
                sb.append(":");
                sb.append(cookie.getValue());
                sb.append("_");
            }
        }
        sb.append("-");
        return sb;
    }
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = null;
        try {
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (ipAddress.equals("127.0.0.1")) {
                    // 根据网卡取本机配置的IP
                    InetAddress inet = null;
                    try {
                        inet = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        log.error("getIpAddr",e);
                    }
                    ipAddress = inet.getHostAddress();
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
                // = 15
                if (ipAddress.indexOf(",") > 0) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
                }
            }
        } catch (Exception e) {
            log.error("getIpAddr",e);
            ipAddress="";
        }
        // ipAddress = this.getRequest().getRemoteAddr();

        return ipAddress;
    }

}
