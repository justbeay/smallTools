package com.tool.redismgr;

import com.sampullara.cli.Args;
import com.tool.utils.RedisUtil;
import com.tool.utils.StringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Administrator on 2017/10/20.
 */
public class RedisConsoleMgmt {

    private static Logger logger = LoggerFactory.getLogger(RedisUtil.class);
    private ExeResultCallback resultCallback;
    private Jedis connection = null;

    public RedisConsoleMgmt(){
        resultCallback = new ExeResultCallback();
    }

    /**
     * 覆盖默认执行结果回调函数
     * @param resultCallback
     */
    public void setResultCallback(ExeResultCallback resultCallback){
        this.resultCallback = resultCallback;
    }

    public void start(String... params){
        if(connection == null){
            connection = RedisUtil.getConnection();
        }
        RedisMgmtCommand command = this.init(params);
        if(command.getCommand() != null){
            Object result = this.execute(connection, command.getCommand(), command.getParams());
            if(resultCallback != null && result != null){
                resultCallback.callback(true, result);
            }
        }else{
            this.execute(connection, System.in, resultCallback);
        }
    }

    public RedisMgmtCommand init(String... args){
        RedisMgmtCommand command = new RedisMgmtCommand();
        List<String> remains = Args.parse(command, args);
        if(!CollectionUtils.isEmpty(remains)){
            logger.warn("command: {} remained unknown", remains);
        }
        if(command.getHost() != null){
            String[] arr = command.getHost().split(":");
            RedisUtil.setServerHost(arr[0].trim());
            if(arr.length > 1){
                RedisUtil.setServerPort(Integer.valueOf(arr[1]));
            }
        }
        if(command.getPassword() != null){
            RedisUtil.setServerPassword(command.getPassword());
        }
        if(command.getSelectDB() != null){
            RedisUtil.setServerDefaultDb(command.getSelectDB());
        }
        return command;
    }

    public Object execute(Jedis connection, String command, String... args){
        return RedisUtil.proxyInner(connection, command, args);
    }

    public void execute(Jedis connection, InputStream in, ExeResultCallback resultCallback){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        while(true) {
            boolean executeSuccess = false;
            Object executeResult = null;
            try {
                String commandLine = bufferedReader.readLine();
                String[] arr = StringUtil.split(commandLine, '\\');
                if(arr.length == 0) continue;

                String command = arr[0];
                String[] paramArr = new String[arr.length - 1];
                for (int i = 1; i < arr.length; i++) {
                    paramArr[i - 1] = arr[i];
                }
                executeResult = this.execute(connection, command, paramArr);
                executeSuccess = true;
            } catch (IOException e) {
                logger.error("IO error happens in execute, message:{}", e.getMessage());
                executeResult = e;
                System.exit(1);
            } catch (Exception e) {
                logger.error("error happens in execute, message:"+e.getMessage(), e);
                executeResult = e;
            } finally {
                if(resultCallback != null){
                    resultCallback.callback(executeSuccess, executeResult);
                }
            }
        }
    }

    /**
     * 用于程序执行完后的结果回调
     */
    public static class ExeResultCallback{
        /**
         * 回调函数，用于程序执行后的回调处理。可重写
         * @param success 是否执行成功
         * @param data 执行结果，执行错误时为异常星系
         */
        void callback(boolean success, Object data){
            if (success) {
                System.out.println(data == null ? null : data.toString());
            } else {
                System.err.println("error occurs: " + (data == null ? null : ((Exception) data).getMessage()));
            }
        }
    }
}
