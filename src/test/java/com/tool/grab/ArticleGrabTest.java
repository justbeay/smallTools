package com.tool.grab;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

        grabInstance.setContentStructure("#content");
        grabInstance.setTitleStructure("#wrapper > div.content_read > div > div.bookname > h1");
        grabInstance.setPagePrevStructure("#wrapper > div.content_read > div > div.bookname > div.bottem1 > a:nth-child(3)");
        grabInstance.setPageNextStructure("#wrapper > div.content_read > div > div.bookname > div.bottem1 > a:nth-child(5)");
    }

    @Test
    public void testGrabCatalogueTitle() throws Exception{
        String title = grabInstance.grabCatalogueTitle();
        System.out.println(title);
    }

    @Test
    public void testGrabCatalogue() throws Exception{
        List<Catalogue> catalogues = grabInstance.grabCatalogue();
        if(CollectionUtils.isEmpty(catalogues)){
            System.out.println(catalogues);
        }else{
            for(Catalogue catalogue : catalogues){
                System.out.println(JSON.toJSONString(catalogue));
            }
        }
    }

    @Test
    public void testGrabArticle() throws Exception{
        Article article = grabInstance.grabArticle("http://www.biquge5200.com/38_38857/15054739.html");
        System.out.println(JSONObject.toJSONString(article));
    }

    @Test
    public void testGrabArticle1() throws Exception{
        grabInstance.setContentUrl("http://www.biquge5200.com/38_38857/15054739.html");
        grabInstance.setGrabTimeInterval(1000l);
        Article article = grabInstance.grabArticle(3);
        System.out.println(JSONObject.toJSONString(article));
    }

    @Test
    public void testGrabArticle2() throws Exception{
        grabInstance.setContentUrl("http://www.biquge5200.com/38_38857/15054739.html");
        grabInstance.setGrabTimeInterval(500l);
        List<Article> articles = grabInstance.grabArticle(2, 5);
        System.out.println(JSONObject.toJSONString(articles));
    }
}
