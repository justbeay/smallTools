package com.tool.grab;

/**
 * Created by Administrator on 2017/10/13.
 */
public enum SpamStrategyEnum implements Comparable<SpamStrategyEnum> {

    MATCH_EQUAL_DEL_LINE("完全匹配"),
    MATCH_CONTAINS_DEL_SELF("包含后删除匹配关键字自身"),
    MATCH_CONTAINS_DEL_LINE("包含后删除匹配行"),
    MATCH_EQUAL_DEL_REMAINS("完全匹配后删除剩余所有"),
    MATCH_PREFIX_DEL_REMAINS("前缀匹配后删除剩余所有"),
    MATCH_CONTAINS_DEL_REMAINS("包含后删除剩余所有")
    ;

    private String desc;
    private SpamStrategyEnum(String desc){
        this.desc = desc;
    }

    /**
     * 是否需要继续
     * @return
     */
    public boolean needContinue(){
        return !this.equals(MATCH_EQUAL_DEL_REMAINS) && !this.equals(MATCH_CONTAINS_DEL_REMAINS) && !this.equals(MATCH_PREFIX_DEL_REMAINS);
    }

    public boolean deleteLine(){
        return this.equals(MATCH_EQUAL_DEL_LINE) || this.equals(MATCH_CONTAINS_DEL_LINE)
                || this.equals(MATCH_PREFIX_DEL_REMAINS) || this.equals(MATCH_EQUAL_DEL_REMAINS);
    }

    public boolean matchEqual(){
        return this.equals(MATCH_EQUAL_DEL_LINE) || this.equals(MATCH_EQUAL_DEL_REMAINS);
    }
}
