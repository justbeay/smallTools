package com.tool.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.util.SafeEncoder;

import java.util.*;

/**
 * Created by Administrator on 2017/10/19.<br/>
 * for redis command usage, please refer to <a href="http://doc.redisfans.com/">doc</a>
 */
public class RedisUtil {

    /** 字符串 */
    public static final String TYPE_STRING = "string";
    /** 哈希表 */
    public static final String TYPE_HASH = "hash";
    /** 列表 */
    public static final String TYPE_LIST = "list";
    /** 集合 */
    public static final String TYPE_SET = "set";
    /** 有序集 */
    public static final String TYPE_ZSET = "zset";

    private static Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private volatile static JedisPool jedisPool;

    private static String serverHost = PropertyUtil.getProperty("redis.server.host");
    private static Integer serverPort = PropertyUtil.getProperty("redis.server.port", Integer.class);
    private static Integer serverMaxWait = PropertyUtil.getProperty("redis.server.maxWaitMillis", Integer.class);
    private static Integer serverMaxTotal = PropertyUtil.getProperty("redis.server.maxTotal", Integer.class);
    private static Integer serverMaxIdle = PropertyUtil.getProperty("redis.server.maxIdle", Integer.class);
    private static Boolean serverTestOnBorrow = PropertyUtil.getProperty("redis.server.testOnBorrow", Boolean.class);
    private static Integer serverDefaultDb = PropertyUtil.getProperty("redis.server.defaultDb", Integer.class);
    private static String serverPassword = PropertyUtil.getProperty("redis.server.password");

    public static void setServerHost(String serverHost){
        RedisUtil.serverHost = serverHost;
    }

    public static void setServerPort(Integer serverPort){
        RedisUtil.serverPort = serverPort;
    }

    public static void setServerPassword(String serverPassword){
        RedisUtil.serverPassword = serverPassword;
    }

    public static void setServerDefaultDb(Integer serverDefaultDb){
        RedisUtil.serverDefaultDb = serverDefaultDb;
    }

    public static final Jedis getConnection() {
        Jedis jedis = null;
        try {// 双重检查
            if (jedisPool == null) {
                synchronized (RedisUtil.class) {
                    if (jedisPool == null) {
                        JedisPoolConfig config = new JedisPoolConfig();
                        config.setMaxTotal(serverMaxTotal);
                        config.setMaxIdle(serverMaxIdle);
                        config.setMaxWaitMillis(serverMaxWait);
                        config.setTestOnBorrow(serverTestOnBorrow);
                        serverDefaultDb = (serverDefaultDb==null || serverDefaultDb > 15 || serverDefaultDb < 0) ? 0 : serverDefaultDb;
                        if (StringUtils.isEmpty(serverPassword)) {// 没有设置密码
                            jedisPool = new JedisPool(config, serverHost, serverPort);
                        } else {// 设置密码
                            jedisPool = new JedisPool(config, serverHost, serverPort, serverMaxWait, serverPassword);
                        }
                        logger.info("Init redis connectionPool success, connect info is: {}:{}?defautlDb={}", serverHost, serverPort, serverDefaultDb);
                    }
                }
            }
            jedis = jedisPool.getResource();
            jedis.select(serverDefaultDb);
        } catch (Exception e) {
            logger.error("Get redis connectionPool failed with message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
        return jedis;
    }

    /***
     * 释放资源
     *
     * @param jedis
     */
    public static final void closeResource(Jedis jedis) {
        try {
            if (jedis != null) {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error("Failed to free redis connection with message:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static <T> T proxyInner(Jedis jedis, String method, Class<T> retClazz, Object... params){
        long startTime = System.currentTimeMillis();
        boolean success = true;
        boolean isJedisNull = jedis == null;
        if(isJedisNull){
            jedis = getConnection();
        }
        try {
            // 查找对应的command命令
            Protocol.Command command = null;
            for(Protocol.Command tmpCommand : Protocol.Command.values()){
                if(tmpCommand.name().equals(method.toUpperCase())){
                    command = tmpCommand;
                    break;
                }
            }
            if(command == null){
                throw new IllegalArgumentException("unknown redis command: " + method);
            }
            // 将redis命令参数转化为byte数组（string数组类型的入参类型无法匹配）
            int paramsLen = params == null ? 0 : params.length;
            byte[][] byteParams = new byte[paramsLen][];
            for(int i=0; i<paramsLen; i++){
                byteParams[i] = SafeEncoder.encode(params[i].toString());
            }
            ReflectUtil.invokeMatchedMethodForce(jedis.getClient(), "sendCommand", command, byteParams);
            return (T) parseRedisOutput(jedis.getClient().getAll());
        } catch (Exception e){
            success = false;
            logger.error("Failed to run redis method:{} with params:{}, error message:{}",
                    method, JSON.toJSONString(params), e.getMessage());
            throw new RuntimeException(e);
        } finally {
            processAfterProxy(jedis, method, params, success, startTime, isJedisNull);
        }
    }

    public static Object proxyInner(Jedis jedis, String method, Object... params){
        return proxyInner(jedis, method, Object.class, params);
    }

    /**
     * 解析redisClient返回的结果
     * @param outputObj
     * @return
     */
    private static Object parseRedisOutput(Object outputObj){
        if(outputObj instanceof List){
            List<Object> retList = new ArrayList<Object>();
            for(Object subObj : (List) outputObj){
                retList.add(parseRedisOutput(subObj));
            }
            return retList;
        }else{
            if(outputObj instanceof byte[]){
                return SafeEncoder.encode((byte[]) outputObj);
            }
            return outputObj;
        }
    }

    private static void processAfterProxy(Jedis jedis, String method, Object[] params, boolean success, long startTime, boolean closeFlag){
        if(params == null || params.length == 0){
            logger.info("run redis command:{} {}, use time:{}ms",
                    method, success ? "success" : "failure", System.currentTimeMillis() - startTime);
        }else{
            logger.info("run redis command:{} with params:{} {}, use time:{}ms",
                    method, JSON.toJSONString(params), success ? "success" : "failure", System.currentTimeMillis() - startTime);
        }
        if(closeFlag && jedis != null){
            closeResource(jedis);
        }
    }

    public static <T> T proxy(Jedis jedis, String method, Class<T> retClazz, Object... params){
        long startTime = System.currentTimeMillis();
        boolean success = true;
        boolean isJedisNull = jedis == null;
        if(isJedisNull){
            jedis = getConnection();
        }
        try {
            Object retObj = ReflectUtil.invokeMatchedMethod(jedis, method, params);
            if(retObj == null) return null;
            return (T) retObj;
        } catch (Exception e){
            success = false;
            logger.error("Failed to run redis method:{} with params:{}", method, JSON.toJSONString(params));
            throw new RuntimeException(e);
        } finally {
            processAfterProxy(jedis, method, params, success, startTime, isJedisNull);
        }
    }

    public static Object proxy(Jedis jedis, String method, Object... params){
        return proxy(jedis, method, Object.class, params);
    }

    public static <T> T proxy(String method, Class<T> retClazz, Object... params){
        return proxy(null, method, retClazz, params);
    }

    public static Object proxy(String method, Object... params){
        return proxy(null, method, Object.class, params);
    }
}
