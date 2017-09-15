package com.tool.logmonitor;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/9/13.
 */
public class IbatisLogRecord {

    private static final Pattern paramRegex = Pattern.compile("^(.*)\\((\\w+)\\)$");
    private static final Logger logger = LoggerFactory.getLogger(IbatisLogRecord.class);

    private String prepareSQL;
    private String from;
    private long createTime;
    private int paramNum;
    private List<String> parameters;
    private boolean paramAccept;

    public IbatisLogRecord(String prepareSQL, String from){
        this.prepareSQL = prepareSQL;
        this.from = from;
        this.paramNum = 0;
        String tmpSQL = prepareSQL;
        int pos = -1;
        while((pos = tmpSQL.indexOf('?')) >= 0){
            this.paramNum ++;
            tmpSQL = tmpSQL.substring(pos + 1);
        }
        this.createTime = System.currentTimeMillis();
    }

    public String getPrepareSQL() {
        return prepareSQL;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
        this.paramAccept = true;
    }

    public String getFrom() {
        return from;
    }

    public int getParamNum() {
        return paramNum;
    }

    public long getCreateTime() {
        return createTime;
    }

    /**
     * 是否已接收SQL查询参数
     * @return
     */
    public boolean isParamAccept() {
        return paramAccept;
    }

    /**
     * 是否允许接收SQL查询参数
     * @return 仅在未接收过参数且from和参数个数均匹配时返回true
     */
    public boolean isParamAllowed(List<String> parameters, String from) {
        boolean allowed = !paramAccept &&
                (from == null && this.from == null || from != null && from.equals(this.from));
        if(allowed) {
            if(CollectionUtils.isEmpty(parameters)){
                allowed = this.paramNum == 0;
            }else{
                allowed = this.paramNum == parameters.size();
            }
        }
        return allowed;
    }

    public String getFinalSQL(){
        if(!this.paramAccept) return null;

        String finalSQL = this.prepareSQL;
        int paramNum = this.paramNum;
        while(paramNum > 0){
            String paramReplace = null;
            String parameter = this.parameters.get(this.paramNum - paramNum);
            Matcher paramMatcher = paramRegex.matcher(parameter);
            if(paramMatcher.matches()){
                String paramValue = paramMatcher.group(1).trim();
                String paramType = paramMatcher.group(2).trim().toLowerCase();
                if(Arrays.asList("integer", "long", "float", "double").contains(paramType)){
                    paramReplace = paramValue;
                }else{
                    paramReplace = '\'' + paramValue + '\'';
                }
                finalSQL = finalSQL.replaceFirst("\\?", paramReplace);
            }else{
                logger.warn("parameter:{} cannot be resolved, ignore it", parameter);
            }
            paramNum --;
        }
        return finalSQL;
    }
}
