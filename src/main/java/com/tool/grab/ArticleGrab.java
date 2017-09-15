package com.tool.grab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章抓取<br/>
 * DOM结构占位符(主要是目录抓取是用到)为{起始index}，index可为空，意味着从0开始<br/>
 * Created by Administrator on 2017/9/15.
 */
public class ArticleGrab {

    private String catalogueUrl;  // 目录页URL
    private String catalogueTitleStructure;  // 目录页标题DOM结构
    private String coverImageStructure;  // 封面图DOM结构
    private String catalogueListStructure;  // 目录页文章列表DOM结构
    private String firstArticleUrl;  // 文章第一页URL
    private String titleStructure;  // 文章标题DOM结构
    private String contentStructure;  // 正文DOM结构
    private String pagePrevStructure;  // 文章上一页DOM结构
    private String pageNextStructure;  // 文章下一页DOM结构

    private Map<String, Object> getRequestParam;
    private Map<String, Object> postRequestParam;

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

    public void setCatalogueTitleStructure(String catalogueTitleStructure) {
        this.catalogueTitleStructure = catalogueTitleStructure;
    }

    public void setCoverImageStructure(String coverImageStructure) {
        this.coverImageStructure = coverImageStructure;
    }

    public void setCatalogueListStructure(String catalogueListStructure) {
        this.catalogueListStructure = catalogueListStructure;
    }

    public void setFirstArticleUrl(String firstArticleUrl) {
        this.firstArticleUrl = firstArticleUrl;
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
     * 抓取文章总标题
     * @return
     */
    public String grabTitle(){
        return null;
    }

    /**
     * 抓取目录
     * @return
     */
    public List<Catalogue> grabCatalogue(){
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
}
