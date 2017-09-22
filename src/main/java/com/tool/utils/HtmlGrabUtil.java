package com.tool.utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HTML网页抓取工具类<br/>
 * Created by Administrator on 2017/9/22.
 */
public class HtmlGrabUtil {

    /**
     * 格式化选择器
     * @param selector 选择器
     * @param params 待替换选择器中占位符的参数列表
     * @return
     */
    public static String formatSelector(String selector, Object... params){
        // 对于要替换参数的，替换选择器中的 nth-child，以免误匹配
        String formatResult = params.length == 0 ? selector : selector.replace("nth-child", "nth-of-type");
        for(int i=0; i<params.length; i++) {
            String tmpResult = formatResult.replaceFirst("\\{\\}", params[i].toString());
            if(tmpResult.equals(formatResult)) break;

            formatResult = tmpResult;
        }
        return formatResult;
    }

    /**
     * 通过选择器选择html节点
     * @param document
     * @param selector
     * @return
     */
    public static Element get(Document document, String selector){
        Elements elements = document.select(selector);
        if(elements == null || elements.isEmpty()) return null;
        Iterator<Element> iterator = elements.iterator();
        return iterator.next();
    }

    public static List<Element> getAll(Document document, String selector){
        List<Element> resultList = new ArrayList<Element>();
        Elements elements = document.select(selector);
        if(elements != null && !elements.isEmpty()){
            Iterator<Element> iterator = elements.iterator();
            while(iterator.hasNext()){
                resultList.add(iterator.next());
            }
        }
        return resultList;
    }

    public static String getAsText(Document document, String selector){
        Element element = get(document, selector);
        return element == null ? null : element.html();
    }

    /**
     * 获取选择器所属节点的html文本
     * @param document
     * @param selector
     * @return
     */
    public static String getInnerHtml(Document document, String selector){
        Element element = get(document, selector);
        return element == null ? null : element.text();
    }

}
