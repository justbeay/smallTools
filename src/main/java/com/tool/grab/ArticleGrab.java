package com.tool.grab;

import com.tool.utils.HtmlGrabUtil;
import com.tool.utils.HttpUtil;
import com.tool.utils.PropertyUtil;
import com.tool.utils.RandomUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.utils.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 文章抓取<br/>
 * DOM结构占位符(主要是目录抓取是用到)为{起始index}，index可为空，意味着从0开始<br/>
 * Created by Administrator on 2017/9/15.
 */
public class ArticleGrab {

    private String catalogueUrl;  // 目录页URL
    private String catalogueUrlReqMethod;  // 目录页URL请求method
    private String catalogueTitleStructure;  // 目录页标题DOM结构
    private String coverImageStructure;  // 封面图DOM结构
    private String catalogueListStructure;  // 目录页文章列表DOM结构
    private String contentUrl;  // 文章正文URL（最好是第一页）
    private String contentUrlReqMethod;  // 文章正文URL请求method
    private String titleStructure;  // 文章标题DOM结构
    private String contentStructure;  // 正文DOM结构
    private String pagePrevStructure;  // 文章上一页DOM结构
    private String pageNextStructure;  // 文章下一页DOM结构

    private Map<String, Object> getRequestParam;
    private Map<String, Object> postRequestParam;
    private Document cataloguePageDoc;
    private Document curContentPageDoc;

    private static final String DEFAULT_ENCODING = "UTF8";
    private static Logger logger = LoggerFactory.getLogger(ArticleGrab.class);

    public ArticleGrab(){

    }

    public void addGetRequestParam(String paramKey, Object paramValue){
        if(this.getRequestParam == null){
            this.getRequestParam = new HashMap<String, Object>();
        }
        this.getRequestParam.put(paramKey, paramValue);
    }

    public void addPostRequestParam(String paramKey, Object paramValue){
        if(this.postRequestParam == null){
            this.postRequestParam = new HashMap<String, Object>();
        }
        this.postRequestParam.put(paramKey, paramValue);
    }

    public void setCatalogueUrl(String catalogueUrl) {
        this.catalogueUrl = catalogueUrl;
    }

    public void setCatalogueUrl(String catalogueUrl, String requestMethod) {
        this.catalogueUrl = catalogueUrl;
        this.catalogueUrlReqMethod = requestMethod;
    }

    public void setCatalogueTitleStructure(String catalogueTitleStructure) {
        this.catalogueTitleStructure = catalogueTitleStructure;
    }

    public void setCoverImageStructure(String coverImageStructure) {
        this.coverImageStructure = coverImageStructure;
    }

    public void setCatalogueListStructure(String catalogueListStructure) {
        this.catalogueListStructure = catalogueListStructure;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public void setContentUrl(String contentUrl, String requestMethod) {
        this.contentUrl = contentUrl;
        this.contentUrlReqMethod = requestMethod;
    }

    public void setTitleStructure(String titleStructure) {
        this.titleStructure = titleStructure;
    }

    public void setContentStructure(String contentStructure) {
        this.contentStructure = contentStructure;
    }

    public void setPagePrevStructure(String pagePrevStructure) {
        this.pagePrevStructure = pagePrevStructure;
    }

    public void setPageNextStructure(String pageNextStructure) {
        this.pageNextStructure = pageNextStructure;
    }

    /**
     * 抓取目录页标题（大标题或总标题）
     * @return
     */
    public String grabCatalogueTitle() throws Exception{
        this.loadCataloguePage(false);
        if(cataloguePageDoc != null && catalogueTitleStructure != null){
            return HtmlGrabUtil.getInnerHtml(cataloguePageDoc, catalogueTitleStructure);
        }
        return null;
    }

    /**
     * 抓取目录
     * @return
     */
    public List<Catalogue> grabCatalogue() throws Exception{
        this.loadCataloguePage(false);
        if(cataloguePageDoc != null && catalogueListStructure != null){
            List<Catalogue> resultList = new ArrayList<Catalogue>();
            for(int i=1; ; i++){
                String selector = HtmlGrabUtil.formatSelector(catalogueListStructure, i);
                Element itemEle = HtmlGrabUtil.get(cataloguePageDoc, selector);
                if(itemEle == null) break;
                Catalogue catalogue = new Catalogue();
                catalogue.setTitle(itemEle.text());
                catalogue.setUrl(itemEle.attr("href"));
                resultList.add(catalogue);
                logger.debug("The {}th catalogue was added...", i);
            }
            return resultList;
        }
        return null;
    }

    /**
     * 抓取文章
     * @param currentPage 待抓取文章页号（从`firstArticleUrl`文章页号开始算起）
     * @return
     */
    public Article grabArticle(Integer currentPage){
        return null;
    }

    /**
     * 抓取文章（批量）
     * @param startPage 待抓取起始页
     * @param endPage 待抓取的最后一页
     * @return
     */
    public List<Article> grabArticle(Integer startPage, Integer endPage){
        return null;
    }

    /**
     * 保存http请求内容至本地
     * @param url
     * @param content
     * @return 保存的文件路径
     */
    private String saveHttpRequestContent(String url, String content) throws IOException{
        String savePath = PropertyUtil.getProperty("path.save.tmp", "/tmp");
        File saveFile = new File(String.format("%s/%s_%s_%s.htm",
                savePath, DateUtils.formatDate(new Date(), "yyyyMMddHHmmssSSS"),
                new URL(url).getHost(), RandomUtil.getRandomNumberStr(4)));
        FileUtils.write(saveFile, content, DEFAULT_ENCODING);
        return saveFile.getPath();
    }

    /** 加载目录页 */
    private void loadCataloguePage(boolean force) throws IOException{
        if(cataloguePageDoc != null && !force) return;

        String pageContent = null;
        if(catalogueUrlReqMethod != null && "post".equals(catalogueUrlReqMethod.toLowerCase())){
            pageContent = HttpUtil.post(catalogueUrl, null);
        }else{
            pageContent = HttpUtil.get(catalogueUrl);
        }
        this.saveHttpRequestContent(catalogueUrl, pageContent);
        cataloguePageDoc = Jsoup.parse(pageContent);
    }

    /** 加载文章正文页 */
    private void loadContentPage(String curContentUrl) throws IOException{
        String pageContent = null;
        if(contentUrlReqMethod != null && "post".equals(contentUrlReqMethod.toLowerCase())){
            pageContent = HttpUtil.post(curContentUrl, null);
        }else{
            pageContent = HttpUtil.get(curContentUrl);
        }
        this.saveHttpRequestContent(curContentUrl, pageContent);
        curContentPageDoc = Jsoup.parse(pageContent);
    }

}
