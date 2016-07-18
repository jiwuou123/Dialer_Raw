package com.android.dialer.dialpad;

/**
 * Created by Administrator on 2016/7/9.
 */

public class LatinAndChineseSmartDialMap extends LatinSmartDialMap{
    private HanziToPinyin pinyinTool;
    public LatinAndChineseSmartDialMap(){
        super();
        pinyinTool = HanziToPinyin.getInstance();
    }
    @Override
    public char normalizeCharacter(char ch) {
        if(pinyinTool.isChinese(ch)){
            return pinyinTool.getAcronymToken(ch).target.charAt(0);
        }
        return super.normalizeCharacter(ch);
    }
}
