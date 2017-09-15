package com.tool.grab;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by Administrator on 2017/9/15.
 */
public class ArticleGrabTest {

    private ArticleGrab grabInstance;

    @Before
    public void init(){
        grabInstance = new ArticleGrab();
        grabInstance.setCatalogueUrl("http://www.biquge5200.com/38_38857/");
        grabInstance.setCatalogueTitleStructure("#info > h1");
        grabInstance.setCatalogueListStructure("#list > dl > dd:nth-child({}) > a");
    }

    @Test
    public void testGrabTitle(){
        String title = grabInstance.grabTitle();
        System.out.println(title);
    }

}
