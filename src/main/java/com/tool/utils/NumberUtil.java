package com.tool.utils;

import java.util.*;

/**
 * Created by Administrator on 2017/10/10.
 */
public class NumberUtil {

    private static char[] simplifiedChineseDigits = new char[]{'○', '一', '二', '三', '四', '五', '六', '七', '八', '九'};
    private static char[] traditionalChineseDigits = new char[]{'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};

    private static Map<Character, Double> simplifiedChineseDigitScales;
    private static Map<Character, Double> traditionalChineseDigitScales;

    static {
        simplifiedChineseDigitScales = new HashMap<Character, Double>();
        simplifiedChineseDigitScales.put('分', 0.01);
        simplifiedChineseDigitScales.put('角', 0.1);
        simplifiedChineseDigitScales.put('点', 1.0);
        simplifiedChineseDigitScales.put('元', 1.0);
        simplifiedChineseDigitScales.put('十', 10.0);
        simplifiedChineseDigitScales.put('百', 100.0);
        simplifiedChineseDigitScales.put('千', 1000.0);
        simplifiedChineseDigitScales.put('万', 10000.0);
        simplifiedChineseDigitScales.put('亿', 100000000.0);
        traditionalChineseDigitScales = new HashMap<Character, Double>();
        traditionalChineseDigitScales.put('分', 0.01);
        traditionalChineseDigitScales.put('角', 0.1);
        traditionalChineseDigitScales.put('点', 1.0);
        traditionalChineseDigitScales.put('元', 1.0);
        traditionalChineseDigitScales.put('拾', 10.0);
        traditionalChineseDigitScales.put('廿', 20.0);
        traditionalChineseDigitScales.put('卅', 30.0);
        traditionalChineseDigitScales.put('卌', 40.0);
        traditionalChineseDigitScales.put('佰', 100.0);
        traditionalChineseDigitScales.put('仟', 1000.0);
        traditionalChineseDigitScales.put('万', 10000.0);
        traditionalChineseDigitScales.put('亿', 100000000.0);
        traditionalChineseDigitScales.put('兆', 10000000000000.0);
    }

    /**
     * 转换中文数字（仅支持至16位有效数字）
     * @param number
     * @param simplified
     * @return
     */
    public static Double convertChineseNumber(CharSequence number, Boolean simplified){
        Double result = 0.0;
        Double pointScale = 0.0;
        Double pointResult = 0.0;
        Double lastPointResult = 0.0;
        boolean isPrevDigit = false;
        boolean isFullDigit = true;
        for(int i=0; i < number.length(); i++){
            Number curNumber = convertChineseNumberSingle(number.charAt(i), simplified);
            if(curNumber == null) throw new IllegalArgumentException(number + " is not a valid number");

            if(curNumber instanceof Integer){  // 碰到数字
                if(pointScale != 0){  // 小数位
                    pointResult = pointResult * 10 + (int) curNumber;
                    pointScale *= 10;
                    lastPointResult = lastPointResult * 10 + (int) curNumber;
                }else{  // 整数位
                    if(isPrevDigit && isFullDigit){
                        result = result * 10 + (int) curNumber;
                    }else if(isPrevDigit && (int) curNumber > 0){
                        int times = 1;
                        for(long tmp=result.longValue(); tmp % 10 > 0 && tmp > 0; tmp /= 10) times *= 10;
                        result = times == 1 ? result + (int) curNumber
                                : (result - result % times) + (result % times) * 10 + (int) curNumber;
                    }else{
                        result += (int) curNumber;
                    }
                }
            }else{  // 进制处理
                if(pointScale != 0){  // 第二个小数位后的进制处理
                    if((double) curNumber < 1){
                        double oldPointScale = pointScale;
                        pointScale = 1 / (double) curNumber;
                        if(lastPointResult - pointResult != 0) {
                            pointResult = (pointResult - lastPointResult) * pointScale / oldPointScale + lastPointResult;
                        }
                        lastPointResult = 0.0;
                    }else{  // 小数后不接受10及以上进制
                        throw new IllegalArgumentException("char at " + i + ":'"+ number.charAt(i) + "' was unacceptable.");
                    }
                }else{
                    if((double) curNumber <= 1){  // 第一位小数
                        if(pointScale != 0 && result != 0) {
                            pointResult = result / ((double) curNumber * 10);
                            result = 0.0;
                        }
                        pointScale = 1 / (double) curNumber;
                    }else{ // 整数位进制处理
                        if(isPrevDigit && result > (double) curNumber){
                            int times = 1;
                            while(result % (times * 10) == 0) times *= 10;
                            result = (result - result % (times * 10)) + (result % (times * 10)) * (double) curNumber;
                        }else {
                            result = result == 0 ? (double) curNumber : result * (double) curNumber;
                        }
                    }
                }
            }
            isPrevDigit = curNumber instanceof Integer;
            isFullDigit = isFullDigit && isPrevDigit;
        }
        return pointScale == 0 ? result : result + pointResult / pointScale;
    }

    /**
     * 转换中文数字
     * @param str
     * @return
     */
    public static Double convertChineseNumber(CharSequence str){
        return convertChineseNumber(str, null);
    }

    private static Number convertChineseNumberSingle(char digit, Boolean simplified){
        if(simplified == null || simplified){  // 搜索简体
            for(int i=0; i<simplifiedChineseDigits.length; i++){
                if(simplifiedChineseDigits[i] == digit){
                    return i;
                }
            }
            for(Map.Entry<Character, Double> entry : simplifiedChineseDigitScales.entrySet()){
                if(entry.getKey() == digit){
                    return entry.getValue();
                }
            }
        }
        if(simplified == null || !simplified){  // 搜索繁体
            for(int i=0; i<traditionalChineseDigits.length; i++){
                if(traditionalChineseDigits[i] == digit){
                    return i;
                }
            }
            for(Map.Entry<Character, Double> entry : traditionalChineseDigitScales.entrySet()){
                if(entry.getKey() == digit){
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static boolean isChineseNumber(CharSequence str, Boolean simplified){
        for(int i=0; i<str.length(); i++){
            Number curNumber = convertChineseNumberSingle(str.charAt(i), simplified);
            if(curNumber == null) return false;
        }
        return true;
    }

    private static boolean isChineseNumber(CharSequence str){
        return isChineseNumber(str, null);
    }

    public static String[] splitChineseNumber(CharSequence str, Boolean simplified){
        List<String> resultList = new ArrayList<String>();
        StringBuilder resultStr = new StringBuilder();
        for(int i=0; i<str.length(); i++){
            if(convertChineseNumberSingle(str.charAt(i), simplified) != null){
                resultStr.append(str.charAt(i));
            }else if(resultStr.length() > 0){
                resultList.add(resultStr.toString());
                resultStr.delete(0, resultStr.length());
            }
        }
        String[] resultArr = new String[resultList.size()];
        return resultList.toArray(resultArr);
    }

    public static String[] splitChineseNumber(CharSequence str){
        return splitChineseNumber(str, null);
    }
}
