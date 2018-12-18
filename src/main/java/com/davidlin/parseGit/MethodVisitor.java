package com.davidlin.parseGit;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;

/**
 * @program: mavendemo
 * @description:
 * @author: davidlin
 * @create: 2018-12-13 17:53
 **/

public class MethodVisitor extends VoidVisitorAdapter<Object> {

    private ArrayList<MethodVO> methodList = new ArrayList<MethodVO>();

    public void visit(MethodDeclaration n, Object object) {
        MethodVO entity = new MethodVO(n.getName(), n.getBeginLine(), n.getEndLine(), n.getParameters());
        methodList.add(entity);
        super.visit(n, object);
    }

    public ArrayList<MethodVO> getMethodList() {
        return this.methodList;
    }
}
