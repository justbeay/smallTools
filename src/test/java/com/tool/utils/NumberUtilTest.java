package com.tool.utils;

import org.junit.Test;

/**
 * Created by Administrator on 2017/10/10.
 */
public class NumberUtilTest {

    private static void testConvertChineseNumber(String str){
        long startTime = System.currentTimeMillis();
        Double result = NumberUtil.convertChineseNumber(str);
        System.out.println("result:"+result+", user time:" + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Test
    public void testConvertChineseNumberBatch(){
        testConvertChineseNumber("一千三百万");
        testConvertChineseNumber("一千零三十四万");
        testConvertChineseNumber("一万零三百四十万亿");
        testConvertChineseNumber("一万零三百四十万亿零八");
        testConvertChineseNumber("一万零三百四十万亿零八千");
        testConvertChineseNumber("一千零四十亿八千万零二百七十六");
        testConvertChineseNumber("五百三十四");
        testConvertChineseNumber("五百零四");
        testConvertChineseNumber("五千零三四");

        testConvertChineseNumber("万");
        testConvertChineseNumber("百二十");

        testConvertChineseNumber("四");
        testConvertChineseNumber("二九");
        testConvertChineseNumber("三零二九");
        testConvertChineseNumber("三零零二四零九");

        testConvertChineseNumber("四点");
        testConvertChineseNumber("四元八角三分");
        testConvertChineseNumber("四元八角二三分");
        testConvertChineseNumber("四点零");
        testConvertChineseNumber("四点零零");
        testConvertChineseNumber("四点零八三四五");
    }

    @Test
    public void testSplitChineseNumber(){
        String[] strArr= new String[]{
                "测试。。。",
                "第一节",
                "第十二节五十二章",
                "第十节 第五三章"
        };
        StringBuilder printStr = new StringBuilder();
        for(String str : strArr){
            printStr.append("{");
            String[] resultArr = NumberUtil.splitChineseNumber(str);
            for(int i=0; i<resultArr.length; i++){
                printStr.append(" \"").append(resultArr[i]).append("\"");
                if(i != resultArr.length - 1) {
                    printStr.append(",");
                }
            }
            printStr.append(" }\n");
        }
        System.out.println(printStr);
    }
}
