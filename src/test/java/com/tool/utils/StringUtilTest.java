package com.tool.utils;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

/**
 * Created by Administrator on 2017/12/14.
 */
public class StringUtilTest {

    @Test
    public void testIndexOf(){
        int pos = StringUtil.indexOf("abcdefg,123", ',', 0, '\\');
        System.out.println(pos);
        pos = StringUtil.indexOf("abcdefg\\,123", ',', 0, '\\');
        System.out.println(pos);
        pos = StringUtil.indexOf("abcdefg\\\\,123", ',', 0, '\\');
        System.out.println(pos);
        pos = StringUtil.indexOf("abcdefg\\\\\\,123", ',', 0, '\\');
        System.out.println(pos);
    }

    @Test
    public void testSplit(){
        String[] resultArr = StringUtil.split(" ", '\\');
        System.out.println(JSON.toJSONString(resultArr));
        resultArr = StringUtil.split("abcefg  ", '\\');
        System.out.println(JSON.toJSONString(resultArr));
        resultArr = StringUtil.split("abc\\e\\ fg   agc\\\\\t123", '\\');
        System.out.println(JSON.toJSONString(resultArr));

        resultArr = StringUtil.split("abc\te\\\tfg   agc\\\\ 123", new char[]{'\t'}, '\\');
        System.out.println(JSON.toJSONString(resultArr));
    }
}
