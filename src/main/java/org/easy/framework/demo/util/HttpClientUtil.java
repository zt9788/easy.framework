package org.easy.framework.demo.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * by zhangtong
 * @param <T>
 */
@Slf4j
@Component
public class HttpClientUtil<T> {



    public <T> T postHttpResponse(String url,Object entity,Boolean isInUri,Class<T> clazz) throws URISyntaxException, IOException {
        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 创建URI对象，并且设置请求参数
        URI uri = null ;
        URIBuilder builder = new URIBuilder(url);
        UrlEncodedFormEntity formEntity = null;
        if(entity != null) {
            if (isInUri) {
                JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(entity));
                object.keySet().forEach(item -> {
                    log.debug("param:{}-{}", object.get(item).toString(), item);
                    builder.addParameter(item, object.get(item).toString());
                });
            } else {
                // 根据开源中国的请求需要，设置post请求参数
                List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
                JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(entity));
                object.keySet().forEach(item -> {
                    parameters.add(new BasicNameValuePair(item, object.get(item).toString()));
                });
                // 构造一个form表单式的实体
                formEntity = new UrlEncodedFormEntity(parameters);
            }
        }
        uri = builder.build();
        log.debug("uri={}",uri);

        // 创建http GET请求
        HttpPost httpPost = new HttpPost(uri);


        // 将请求实体设置到httpPost对象中
        if(formEntity != null)
            httpPost.setEntity(formEntity);

        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析响应数据
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                T obj = JSONObject.parseObject(content,clazz);

                log.debug(content);
                return obj;
            }
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
        }
        return (T)null;
    }


    public <T> T postHttpResponse(String url, Object entity, Boolean isInUri
            , TypeReference<T> clazz) throws URISyntaxException, IOException {
        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 创建URI对象，并且设置请求参数
        URI uri = null ;
        URIBuilder builder = new URIBuilder(url);
        UrlEncodedFormEntity formEntity = null;
        if(entity != null) {
            if (isInUri) {
                JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(entity));
                object.keySet().forEach(item -> {
//                    log.debug("param:{}-{}", object.get(item).toString(), item);
                    builder.addParameter(item, object.get(item).toString());
                });
            } else {
                // 根据开源中国的请求需要，设置post请求参数
                List<NameValuePair> parameters = new ArrayList<NameValuePair>(0);
                JSONObject object = JSONObject.parseObject(JSONObject.toJSONString(entity));
                object.keySet().forEach(item -> {
                    parameters.add(new BasicNameValuePair(item, object.get(item).toString()));
                });
                // 构造一个form表单式的实体
                formEntity = new UrlEncodedFormEntity(parameters);
            }
        }
        uri = builder.build();
        log.debug("uri={}",uri);

        // 创建http GET请求
        HttpPost httpPost = new HttpPost(uri);


        // 将请求实体设置到httpPost对象中
        if(formEntity != null)
            httpPost.setEntity(formEntity);

        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析响应数据
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                T obj = JSONObject.parseObject(content,clazz);

                log.debug(content);
                return obj;
            }
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
        }
        return (T)null;
    }


    public <T> T postHttpResponseByJson(String url, Object entity
            , TypeReference<T> clazz) throws URISyntaxException, IOException {
        // 创建Httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 创建URI对象，并且设置请求参数
        URI uri = null ;
        URIBuilder builder = new URIBuilder(url);
        UrlEncodedFormEntity formEntity = null;

        uri = builder.build();
        log.debug("uri={}",uri);

        // 创建http GET请求
        HttpPost httpPost = new HttpPost(uri);
        StringEntity requestEntity = new StringEntity(JSONObject.toJSONString(entity),"utf-8");
        requestEntity.setContentEncoding("UTF-8");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setEntity(requestEntity);

        // 将请求实体设置到httpPost对象中
        if(formEntity != null)
            httpPost.setEntity(formEntity);

        CloseableHttpResponse response = null;
        try {
            // 执行请求
            response = httpclient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析响应数据
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                T obj = JSONObject.parseObject(content,clazz);

                log.debug(content);
                return obj;
            }
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
        }
        return (T)null;
    }
}
