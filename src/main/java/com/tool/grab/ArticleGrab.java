package com.tool.grab;

import com.tool.utils.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private Long grabTimeInterval;  // 两次抓取之间的时间间隔（防止抓取过快导致服务器报错，默认不停顿），单位毫秒
    private Map<String, SpamStrategyEnum> spamRules;

    private Map<String, Object> getRequestParam;
    private Map<String, Object> postRequestParam;
    private Document cataloguePageDoc;
    private Document curContentPageDoc;
    private Long lastGrabTime;  // 上次网页抓取的时间戳

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

    public void setGrabTimeInterval(Long grabTimeInterval) {
        this.grabTimeInterval = grabTimeInterval;
    }

    /**
     * 增加垃圾词汇匹配规则
     * @param word 关键字
     */
    public void addSpamRule(String word){
        this.addSpamRule(word, SpamStrategyEnum.MATCH_CONTAINS_DEL_SELF);
    }

    /**
     * 增加垃圾词汇匹配规则
     * @param word 关键字
     * @param strategy 策略
     */
    public void addSpamRule(String word, SpamStrategyEnum strategy){
        if(this.spamRules == null){
            this.spamRules = new HashMap<String, SpamStrategyEnum>();
        }
        this.spamRules.put(word, strategy);
    }

    /**
     * 反垃圾处理
     * @param str 待处理文本
     * @return
     */
    private String processSpam(String str){
        StringBuilder buffer = new StringBuilder(str.length());
        int startPos = 0;
        while(startPos < str.length()){
            int endPos = str.indexOf('\n', startPos);
            if(endPos == -1){
                endPos = str.length();
            }
            String lineStr = str.substring(startPos, endPos);
            // 执行反垃圾策略匹配逻辑
            String keyword = null;
            SpamStrategyEnum strategy = null;
            for(Map.Entry<String, SpamStrategyEnum> entry : this.spamRules.entrySet()){
                String tmpKeyword = entry.getKey();
                SpamStrategyEnum tmpStrategy = entry.getValue();
                boolean matched = tmpStrategy.matchEqual() ? (tmpKeyword.isEmpty() ? lineStr.trim().isEmpty() : lineStr.trim().equals(tmpStrategy))
                                            : lineStr.contains(tmpKeyword);
                if(matched){  // 匹配到规则
                    if(!tmpStrategy.needContinue() || strategy == null || !strategy.deleteLine()){  // 匹配到更高级的规则
                        strategy = tmpStrategy;
                        keyword = tmpKeyword;
                    }
                    if(!strategy.needContinue()) break;  // 无需继续处理
                }
            }
            if(strategy != null){  // 根据匹配到的最高级规则进行文本处理
                if(!strategy.deleteLine()){
                    buffer.append(SpamStrategyEnum.MATCH_CONTAINS_DEL_REMAINS.equals(strategy)
                                ? lineStr.substring(0, lineStr.indexOf(keyword))
                                : lineStr.replace(keyword, "")).append('\n');
                }
                if(!strategy.needContinue()) break;  // 无需继续处理
            }else{
                buffer.append(lineStr).append('\n');
            }
            // 更新下一行的起始位置
            startPos = endPos + 1;
        }
        return buffer.length() == 0 ? "" : buffer.deleteCharAt(buffer.length() - 1).toString();  // 删除末位换行
    }

    /**
     * 抓取目录页标题（大标题或总标题）
     * @return
     */
    public String grabCatalogueTitle() throws IOException{
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
    public List<Catalogue> grabCatalogue() throws IOException{
        this.loadCataloguePage(false);
        if(cataloguePageDoc != null && catalogueListStructure != null){
            long startTime = System.currentTimeMillis();
            logger.info("start to get catalogues...");
            List<Catalogue> resultList = new ArrayList<Catalogue>();
            String parentSelector = null;
            Element parentElement = null;
            for(int i=1; ; i++){
                String selector = HtmlGrabUtil.formatSelector(catalogueListStructure, i);
                if(parentElement == null) {
                    parentSelector = HtmlGrabUtil.getCommonSelector(selector, catalogueListStructure);
                    parentElement = StringUtils.isEmpty(parentSelector) ? cataloguePageDoc : HtmlGrabUtil.get(cataloguePageDoc, parentSelector);
                }
                if(!StringUtils.isEmpty(parentSelector)){  // 有公共父级元素，从父级元素开始搜索（加快速度）
                    selector = selector.substring(parentSelector.length()).trim();
                    if(selector.startsWith(">")) selector = selector.substring(1);
                }
                Element itemEle = HtmlGrabUtil.get(parentElement, selector);
                if(itemEle == null) break;
                Catalogue catalogue = new Catalogue();
                catalogue.setTitle(itemEle.text());
                catalogue.setUrl(itemEle.attr("href"));
                resultList.add(catalogue);
                logger.debug("The {}th catalogue was added...", i);
            }
            logger.info("finish getting catalogues, {} in total, use time:{}ms", resultList.size(), System.currentTimeMillis() - startTime);
            return resultList;
        }
        return null;
    }

    public Article grabArticle(String contentUrl) throws IOException{
        Article article = null;
        this.loadContentPage(contentUrl);
        if(this.curContentPageDoc != null){
            article = new Article();
            if(this.contentStructure != null){
                String content = HtmlGrabUtil.getInnerHtml(this.curContentPageDoc, this.contentStructure);
                content = HtmlGrabUtil.formatLineBreak(content);
                article.setContent(this.processSpam(content));
            }
            if(this.titleStructure != null){
                article.setTitle(HtmlGrabUtil.getInnerHtml(this.curContentPageDoc, this.titleStructure));
            }
        }
        return article;
    }

    /**
     * 抓取文章
     * @param currentPage 待抓取文章页号（从`firstArticleUrl`文章页号开始算起）
     * @return
     */
    public Article grabArticle(Integer currentPage) throws IOException{
        if(this.contentUrl == null){
            throw new RuntimeException("contentUrl must specified.");
        }
        // 从第一页开始根据翻页按钮逐页跳转到指定页
        String curPageUrl=this.contentUrl;
        for(int startPage=1; startPage < currentPage && curPageUrl != null; startPage++){
            this.loadContentPage(curPageUrl);
            curPageUrl = getPageNextUrl();
        }
        // 已获取到目标页链接，开始解析正文
        return curPageUrl == null ? null : this.grabArticle(curPageUrl);
    }

    private String getPageNextUrl(){
        if(this.pageNextStructure == null){
            throw new RuntimeException("pageNextStructure must specified.");
        }
        Element pageNextEle = HtmlGrabUtil.get(this.curContentPageDoc, this.pageNextStructure);
        String resultUrl = null;
        if(pageNextEle != null) {
            resultUrl = pageNextEle.attr("href");
        }
        return resultUrl;
    }

    /**
     * 抓取文章（批量）
     * @param startPage 待抓取起始页
     * @param endPage 待抓取的最后一页
     * @return
     */
    public List<Article> grabArticle(Integer startPage, Integer endPage) throws IOException{
        if(this.contentUrl == null){
            throw new RuntimeException("contentUrl must specified.");
        }
        List<Article> articles = new ArrayList<Article>();
        // 从第一页开始根据翻页按钮逐页跳转到指定页
        String curPageUrl=this.contentUrl;
        for(int tmpPage=1; tmpPage <= endPage && curPageUrl != null; tmpPage++){
            if(tmpPage >= startPage) {  // 待抓取文章页
                articles.add(this.grabArticle(curPageUrl));
            }else{  // 无需抓取，加载页面即可
                this.loadContentPage(curPageUrl);
            }
            curPageUrl = getPageNextUrl();
        }
        return articles;
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
        this.doBeforePageLoad();
        if(catalogueUrlReqMethod != null && "post".equals(catalogueUrlReqMethod.toLowerCase())){
            pageContent = HttpUtil.post(catalogueUrl, null);
        }else{
            pageContent = HttpUtil.get(catalogueUrl);
        }
        if(pageContent == null) throw new RuntimeException("load catalogue page failure.");
        this.saveHttpRequestContent(catalogueUrl, pageContent);
        cataloguePageDoc = Jsoup.parse(pageContent);
    }

    /** 加载文章正文页 */
    private void loadContentPage(String curContentUrl) throws IOException{
        String pageContent = null;
        this.doBeforePageLoad();
        if(contentUrlReqMethod != null && "post".equals(contentUrlReqMethod.toLowerCase())){
            pageContent = HttpUtil.post(curContentUrl, null);
        }else{
            pageContent = HttpUtil.get(curContentUrl);
        }
        if(pageContent == null) throw new RuntimeException("load content page failure.");
        this.saveHttpRequestContent(curContentUrl, pageContent);
        curContentPageDoc = Jsoup.parse(pageContent);
    }

    private void doBeforePageLoad(){
        if(this.lastGrabTime != null && this.grabTimeInterval != null){
            while(true) {
                long waitTime = this.grabTimeInterval - (System.currentTimeMillis() - this.lastGrabTime);
                if(waitTime <= 0){
                    this.lastGrabTime = System.currentTimeMillis();
                    break;
                }
                try {
                    Thread.sleep(waitTime);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }else {
            this.lastGrabTime = System.currentTimeMillis();
        }
    }

}
