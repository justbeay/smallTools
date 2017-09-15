package com.tool.grab;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章<br/>
 * Created by Administrator on 2017/9/15.
 */
public class Article {

    private String title;
    private String content;
    private Integer currentPage;
    private Integer totalPage;
    private List<Article> subArticles;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
    }

    public List<Article> getSubArticles() {
        return subArticles;
    }

    public void addSubArticle(Article subArticle) {
        if(this.subArticles == null){
            this.subArticles = new ArrayList<Article>();
        }
        this.subArticles.add(subArticle);
    }
}
