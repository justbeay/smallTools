package com.tool.utils;

/**
 * Created by Administrator on 2017/9/22.
 */
public class RandomUtil {

    public static String getRandomNumberStr(int length){
        StringBuffer randomStr = new StringBuffer(length);
        String tmpRandom = null;
        for(int i=0; i<length; ) {
            tmpRandom = Integer.toString((int) ((1 + Math.random()) * 1000000)).substring(1); // 取随机生成数字的小数后6位数字
            if(tmpRandom.length() > length - i){
                randomStr.append(tmpRandom.substring(0, length - i));
                break;
            }else{
                randomStr.append(tmpRandom);
                i += tmpRandom.length();
            }
        }
        return randomStr.toString();
    }
}
