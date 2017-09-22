package com.tool.grab;

import com.alibaba.fastjson.JSON;
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
}
