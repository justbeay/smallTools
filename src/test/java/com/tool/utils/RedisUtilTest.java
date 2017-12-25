package com.tool.utils;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2017/10/20.
 */
public class RedisUtilTest {

    @Test
    public void test(){
//        String value = RedisUtil.get("job:asynRun_strategyExecution_1116149");
//        System.out.println(value);
//
//        RedisUtil.del("testhash");
//        Map<String, String> resultMap = new HashMap<String, String>();
//        resultMap.put("name", "test");
//        resultMap.put("password", "lsajf;dsjafl2o34234");
//        RedisUtil.hset("testhash", resultMap);
//        resultMap = RedisUtil.hgetAll("testhash");
//        for(Map.Entry<String, String> entry : resultMap.entrySet()){
//            System.out.println(entry.getKey() + " => " + entry.getValue());
//        }
//
//        Set<String> keys = RedisUtil.keys("*test*");
//        for(String key : keys){
//            System.out.println("key:" + key + ", type:"+RedisUtil.type(key));
//        }
//
//        RedisUtil.del("testhash");
    }

    @Test
    public void testProxy(){
        Object result = null;
        result = RedisUtil.proxy("mget", "USER:DX_USER_CODE", "job:fetchInfoToNeo4j");
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void testProxyInner(){
        Object result = null;
        Jedis conn = RedisUtil.getConnection();
        result = RedisUtil.proxyInner(conn, "hgetAll", "testhash");
        System.out.println(JSON.toJSONString(result));
        result = RedisUtil.proxyInner(conn, "select", 3);
        System.out.println(JSON.toJSONString(result));
        result = RedisUtil.proxyInner(conn, "get", "autoRequestP2pInfo");
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void test1(){
        System.out.println(String[].class);
        System.out.println(String.class);
    }
}
