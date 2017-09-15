package com.tool.grab;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章<br/>
 * Created by Administrator on 2017/9/15.
 */
public class Article {

    private String title;
    private String coverImage;  // 封面图片URL
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

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
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

    /**
     * 添加子章节
     * @param subArticle 待添加章节
     * @param inceaseTotal true时递增totalPage
     */
    public void addSubArticle(Article subArticle, boolean inceaseTotal) {
        if(this.subArticles == null){
            this.subArticles = new ArrayList<Article>();
        }
        this.subArticles.add(subArticle);
    }

    /**
     * 是否为最后一页
     * @return
     */
    public boolean isLastPage(){
        return currentPage >= totalPage;
    }
}
