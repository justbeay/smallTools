package com.tool.runner;

import com.tool.logmonitor.IbatisLogMonitor;

/**
 * Created by Administrator on 2017/9/13.
 */
public class IbatisLogMonitorRunner {

    public static void main(String[] args){
        if(args.length == 0){
            System.err.println("usage: java -jar xxx.jar logFile");
            System.exit(1);
        }else{
            for(String logFile : args){
                IbatisLogMonitor monitor = new IbatisLogMonitor(logFile);
                monitor.start();
            }
        }
    }

}
