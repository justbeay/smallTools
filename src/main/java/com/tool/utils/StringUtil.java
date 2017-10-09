package com.tool.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Created by lee on 2017-09-03.
 */
public class StringUtil {

    /**
     * 获取所有字符串的公共前缀
     */
    public static String getCommonPrefix(String... strs){
        // 获取最短字符串的长度
        int minLength = getMinLength(strs);
        // 计算公共前缀
        if(minLength > 0){
            int i = 0;
            while(i < minLength){
                int j = 0;
                while(j < strs.length){
                    if(strs[0].charAt(i) != strs[j].charAt(i)) break;
                    j ++;
                }
                if(j < strs.length) break;
                i ++;
            }
            return strs[0].substring(0, i);
        }
        return "";
    }

    /**
     * 获取最小长度
     */
    public static int getMinLength(String... strs){
        int minLength = strs.length > 0 ? StringUtils.length(strs[0]) : 0;
        for(int i=1; i<strs.length; i++){
            int curLength = StringUtils.length(strs[i]);
            if(curLength < minLength){
                minLength = curLength;
            }
        }
        return minLength;
    }

}
