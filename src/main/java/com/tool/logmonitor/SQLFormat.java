package com.tool.logmonitor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tool.utils.HttpUtil;
import com.tool.utils.PropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/11.
 */
public class SQLFormat {

    private static final String SQL_FORMAT_URL = "http://www.gudusoft.com/format.php";

    private Logger logger = LoggerFactory.getLogger(SQLFormat.class);
    private Map<String, Object> reqOptions;

    public SQLFormat(){
        reqOptions = new HashMap<String, Object>();
        reqOptions.put("rqst_db_vendor", "mysql");
        reqOptions.put("rqst_output_fmt", "sql");
        Map<String, Object> formatOptions = PropertyUtil.getProperties("sql.format.", true, true);
        reqOptions.put("rqst_formatOptions", JSONObject.toJSONString(formatOptions));
    }

    public String pretty(String sql){
        reqOptions.put("rqst_input_sql", sql);
        String responseStr = HttpUtil.get(SQL_FORMAT_URL, this.reqOptions);
        String formattedSQL = null;
        if(!StringUtils.isEmpty(responseStr)){
            Map jsonObject = JSON.parseObject(responseStr.replaceAll("(^\\()|(\\)$)", ""), Map.class);
            formattedSQL = (String) jsonObject.get("rspn_formatted_sql");
        }
        if(formattedSQL == null){
            logger.error("failed to pretty sql:{}");
            return null;
        }
        return formattedSQL.trim();
    }
}
