package com.tool.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
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

    public static int getMaxLength(String... strs){
        int maxLength = 0;
        for(int i=1; i<strs.length; i++){
            int curLength = StringUtils.length(strs[i]);
            if(curLength > maxLength){
                maxLength = curLength;
            }
        }
        return maxLength;
    }

    public static int indexOf(String str, char ch, int startPos, char escapedChar){
        int pos = str.indexOf(ch, startPos);
        if(pos >= 0){
            int escapeCount = 0;
            int i = pos;
            while(--i > 0 && str.charAt(i) == escapedChar){
                escapeCount ++;
            }
            if(escapeCount % 2 != 0){  // 前面连续单数个转义符
                pos = indexOf(str, ch, pos + 1, escapedChar);
            }
        }
        return pos;
    }

    public static String[] split(String str, char[] splitChars, char escapeChar){
        List<String> resultList = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        boolean isPreEscape = false;
        for(int i=0; i<str.length(); i++){
            char ch = str.charAt(i);
            if(ch == escapeChar){  // 转义字符
                if(isPreEscape){
                    isPreEscape = false;
                    buffer.append(ch);
                }else{
                    isPreEscape = true;
                }
            }else{
                // 判断是否为分隔符
                boolean isSplitChar = false;
                for(int j=0; j<splitChars.length; j++){
                    if(splitChars[j] == ch){
                        isSplitChar = true;
                        break;
                    }
                }
                if(!isSplitChar || isPreEscape){  // 非分隔符或已转义
                    if(!isSplitChar && isPreEscape) {
                        buffer.append(escapeChar);  // 仅可以对分隔符或转义符进行转义
                    }
                    isPreEscape = false;
                    buffer.append(ch);
                }else if(buffer.length() > 0){
                    resultList.add(buffer.toString());
                    buffer.delete(0, buffer.length());
                }
            }
        }
        if(buffer.length() > 0){
            resultList.add(buffer.toString());
        }
        // 返回
        String[] resultArr = new String[resultList.size()];
        return resultList.toArray(resultArr);
    }

    public static String[] split(String str, char escapeChar){
        return split(str, new char[]{' ', '\t', '\n'}, escapeChar);
    }
}
