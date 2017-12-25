package com.tool.runner;

import com.tool.redismgr.RedisConsoleMgmt;

/**
 * Created by Administrator on 2017/12/25.
 */
public class RedisConsoleMgmtRunner {

    public static void main(String[] args){
        RedisConsoleMgmt monitor = new RedisConsoleMgmt();
        monitor.start(args);
    }
}
