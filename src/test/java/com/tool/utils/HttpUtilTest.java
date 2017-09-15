package com.tool.utils;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/14.
 */
public class HttpUtilTest {

    @Test
    public void testGet_1(){
        String result = HttpUtil.get("http://example.com");
        System.out.println(result);
        result = HttpUtil.get("https://wap.12306.cn/mormhweb");
        System.out.println(result);
    }

    @Test
    public void testGet_2(){
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("city", "上海");
        String result = HttpUtil.get("http://www.sojson.com/open/api/weather/json.shtml", params);
        System.out.println(result);
    }

    @Test
    public void testGetWithHeader(){
        Map<String, String> headers = new HashMap<String, String>();
//        headers.put("x-token", "ed25a1ae2757264aa39f8b545137c5a0");
        String result = HttpUtil.getWithHeader("https://www.baidu.com", headers);
        System.out.println(result);
    }

    @Test
    public void testPostWithHeader(){
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("account", "test");
        params.put("password", "61b80f94cdd6d632f7bc38fd9ed91d9c");
        String result = HttpUtil.postWithHeader("https://login.360.cn", null, params);
        System.out.println(JSONObject.parse(result));
    }

}
