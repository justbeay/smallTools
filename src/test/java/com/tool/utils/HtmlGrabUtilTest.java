package com.tool.utils;

import org.junit.Test;

/**
 * Created by Administrator on 2017/10/9.
 */
public class HtmlGrabUtilTest {

    @Test
    public void testFormatText(){
        String title = "hello, world...";
        String content = "  1.ljslfsdfdsf\n<br/><br/>2.lsjo<br wlrwerwe;;d<br><br>3.kalklio4o3p45435<br />\n4.lldsflsdp034534";
        System.out.println(HtmlGrabUtil.formatText(title));
        System.out.println(HtmlGrabUtil.formatText(content));
    }

}
