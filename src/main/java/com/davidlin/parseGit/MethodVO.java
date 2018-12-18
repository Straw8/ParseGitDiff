package com.davidlin.parseGit;

import com.github.javaparser.ast.body.Parameter;

import java.util.List;

/**
 * @program: mavendemo
 * @description:
 * @author: davidlin
 * @create: 2018-12-13 17:51
 **/


public class MethodVO {
    private String name;
    private int beginIndex;
    private int endIndex;
    private List<Parameter> inParam;

    public MethodVO() {
    }

    public MethodVO(String name, int beginIndex, int endIndex, List<Parameter> inParam) {
        this.name = name;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.inParam = inParam;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public List<Parameter> getInParam() {
        return inParam;
    }

    public void setInParam(List<Parameter> inParam) {
        this.inParam = inParam;
    }
}
