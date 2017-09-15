package com.tool.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Administrator on 2017/9/12.
 */
public class PropertyUtil {

    private static List<String> propFiles;
    private static Map<String, Properties> propsMap;
    private static Logger logger = LoggerFactory.getLogger(PropertyUtil.class);

    static {
        propFiles = new ArrayList<String>();
        propFiles.add("sql_format.properties");
        propFiles.add("network.properties");

        propsMap = new LinkedHashMap<String, Properties>();
        for(String propFile : propFiles) {
            try {
                Properties properties = new Properties();
                properties.load(PropertyUtil.class.getClassLoader().getResourceAsStream(propFile));
                propsMap.put(propFile, properties);
            }catch (Exception e){
                logger.error("load properties file:{} error, message:{}", e.getMessage());
            }
        }
    }

    public static String getProperty(String key){
        for(Map.Entry<String, Properties> entry : propsMap.entrySet()){
            Properties properties = entry.getValue();
            if(properties != null && properties.containsKey(key)){
                return (String) properties.get(key);
            }
        }
        return null;
    }

    public static <T> T getProperty(String key, Class<T> retClass){
        String valueStr = getProperty(key);
        if(!StringUtils.isEmpty(valueStr)) {
            if (retClass.equals(Integer.class)) {
                return (T) Integer.valueOf(valueStr);
            } else if (retClass.equals(Long.class)) {
                return (T) Long.valueOf(valueStr);
            } else if (retClass.equals(Float.class)) {
                return (T) Float.valueOf(valueStr);
            } else if (retClass.equals(Double.class)) {
                return (T) Double.valueOf(valueStr);
            } else if (retClass.equals(Boolean.class)) {
                return (T) Boolean.valueOf(valueStr);
            }
        }else if(!retClass.equals(String.class)){
            return null;
        }
        return (T) valueStr;
    }

    public static Map<String, Object> getProperties(String keyPrefix, boolean splitKey, boolean removePrefix){
        Map<String, Object> retMap = new HashMap<String, Object>();
        for(Map.Entry<String, Properties> entry : propsMap.entrySet()) {
            Properties properties = entry.getValue();
            for(Map.Entry<Object, Object> propEntry : properties.entrySet()){
                String propName = (String) propEntry.getKey();
                if(propName.startsWith(keyPrefix)){
                    if(removePrefix){
                        propName = propName.substring(keyPrefix.length());
                    }
                    if(splitKey){
                        String[] segments = propName.split("\\.");
                        Map<String, Object> tmpMap = retMap;
                        for(int i=0; i<segments.length; i++){
                            if(!tmpMap.containsKey(segments[i])){
                                if(i < segments.length - 1) {
                                    tmpMap.put(segments[i], new HashMap<String, Object>());
                                }else{
                                    tmpMap.put(segments[i], parsePropertyValue((String) propEntry.getValue()));
                                }
                            }
                            if(i < segments.length - 1) tmpMap = (Map<String, Object>) tmpMap.get(segments[i]);
                        }
                    }else if(!retMap.containsKey(propName)){
                        retMap.put(propName, parsePropertyValue((String) propEntry.getValue()));
                    }
                }
            }
        }
        return retMap;
    }

    private static Object parsePropertyValue(String value){
        if(value == null) return null;
        else if("true".equals(value) || "false".equals(value)){
            return Boolean.parseBoolean(value);
        }else if(value.matches("^\\d+$")){
            return Integer.parseInt(value);
        }else if(value.matches("^\\d*\\.\\d+$")){
            return Double.parseDouble(value);
        }
        return value;
    }

}
