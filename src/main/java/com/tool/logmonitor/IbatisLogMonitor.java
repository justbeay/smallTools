package com.tool.logmonitor;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/9/12.
 */
public class IbatisLogMonitor {

    private static final Pattern ibatisLogPrefixPattern = Pattern.compile("^.*\\s+DEBUG\\s+((\\w+\\.)*\\w+Mapper\\.\\w+)");
    private static final Pattern ibatisLogPreparePattern = Pattern.compile(ibatisLogPrefixPattern.pattern() + ".+\\s+Preparing:\\s+(.+)$");
    private static final Pattern ibatisLogParamPattern = Pattern.compile(ibatisLogPrefixPattern.pattern() + ".+\\s+Parameters:\\s+(.*)$");
    private static final long sqlParamTimeout = 800;
    private static final Logger logger = LoggerFactory.getLogger(IbatisLogMonitor.class);

    private TailerListener tailerListener = new IbatisLogListener(this);
    private Tailer tailer;
    private Thread tailerThread;
    private List<IbatisLogRecord> ibatisLogRecords;

    public IbatisLogMonitor(String filePath){
        this.tailer = new Tailer(new File(filePath), tailerListener, 500, true);
        ibatisLogRecords = new LinkedList<IbatisLogRecord>();
    }

    public void start(){
        if(tailerThread == null || !tailerThread.isAlive()){
            tailerThread = new Thread(tailer);
//            tailerThread.setDaemon(true);
            tailerThread.start();
        }
    }

    public void stop(){
        tailer.stop();
    }

    private void handleLine(String line){
        if(!line.isEmpty() && line.indexOf("Mapper.") >= 0){  // 先一步过滤，减少正则匹配次数
            // 对疑似ibatis查询日志进行进一步匹配
            Matcher prepareMatcher = ibatisLogPreparePattern.matcher(line);
            if(prepareMatcher.matches()){
                // 匹配到prepareSQL日志
                String prepareSQL = prepareMatcher.group(3).trim();
                String from = prepareMatcher.group(1).trim();
                ibatisLogRecords.add(new IbatisLogRecord(prepareSQL, from));
            }else{ // 非prepareSQL日志，继续检查是否为parameter日志
                Matcher paramMatcher = ibatisLogParamPattern.matcher(line);
                if(paramMatcher.matches()){
                    // 匹配到parameter日志
                    String paramStr = paramMatcher.group(3);
                    String from = paramMatcher.group(1).trim();
                    this.dealIbatisParamLog(paramStr, from);
                }
            }
        }
    }

    private void dealIbatisParamLog(String paramStr, String from){
        // 分割SQL查询参数字符串
        List<String> params = null;
        if(!StringUtils.isEmpty(paramStr)){
            params = new ArrayList<>();
            for(String param : paramStr.split(",")){
                params.add(param.trim());
            }
        }
        // 找到对应的ibatis语句记录
        IbatisLogRecord ibatisLogRecord = null;
        for(Iterator<IbatisLogRecord> it=ibatisLogRecords.iterator(); it.hasNext(); ){
            IbatisLogRecord tmp = it.next();
            if(tmp.isParamAllowed(params, from)){
                if(System.currentTimeMillis() - tmp.getCreateTime() <= sqlParamTimeout){
                    ibatisLogRecord = tmp;
                    break;
                }
                logger.warn("sql:{} might be ignored as it's interval time beyond {}ms", tmp.getPrepareSQL(), sqlParamTimeout);
                it.remove();
            }
        }
        if(ibatisLogRecord != null){
            // 设置查询参数并进行后续处理
            ibatisLogRecord.setParameters(params);
            this.processIbatisLogRecords();
        }else{
            logger.warn("sql param:{} will ignored as it's matched sql wasn't found", params);
        }
    }

    private void processIbatisLogRecords(){
        SQLFormat sqlFormat = new SQLFormat();
        for(Iterator<IbatisLogRecord> it=ibatisLogRecords.iterator(); it.hasNext(); ){
            IbatisLogRecord ibatisLogRecord = it.next();
            if(ibatisLogRecord.isParamAccept()){
                String finalSQL = ibatisLogRecord.getFinalSQL();
                String prettySQL = sqlFormat.pretty(finalSQL);
                logger.info("a sql query:{} was found:\n{}", ibatisLogRecord.getFrom(), prettySQL != null ? prettySQL : finalSQL);
                it.remove();
            }else{
                // 未设置SQL查询参数，退出等待下一次处理
                break;
            }
        }
    }

    public class IbatisLogListener extends TailerListenerAdapter{
        private IbatisLogMonitor monitor;

        public IbatisLogListener(IbatisLogMonitor monitor){
            this.monitor = monitor;
        }

        @Override
        public void handle(String line) {
            try {
                monitor.handleLine(line);
            }catch (Exception e){
                logger.error("error:{} happens handling log line:{}", e.getMessage(), line);
                e.printStackTrace();
            }
        }
    }
}
