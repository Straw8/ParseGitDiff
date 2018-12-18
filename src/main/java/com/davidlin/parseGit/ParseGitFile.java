package com.davidlin.parseGit;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.wickedsource.diffparser.api.model.Hunk;
import org.wickedsource.diffparser.api.model.Range;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: mavendemo
 * @description:
 * @author: davidlin
 * @create: 2018-12-13 17:39
 **/

public class ParseGitFile {

    public static void main(String[] args) {
        List<MethodVO> methodsFromGitFile = getMethodsFromGitFile("../");
    }

    //path 需要解析变更内容的项目的根目录
    public static List<MethodVO> getMethodsFromGitFile(String path){
        List<MethodVO> list = new ArrayList<MethodVO>();
        try {
            //执行diff命令，读取diff文件,获取改动行
            List<Map> mapList = parseDiffLines(execCommand("git diff", path));
            for (Map map : mapList) {
                if (map.containsKey("fromFileName")){
                    //根据解析结果获取diff文件名称
                    String fromFileName = map.get("fromFileName").toString();
                    if (fromFileName.endsWith(".java")){
                        //读取源文件内容
                        File file = new File(path+fromFileName.substring(fromFileName.indexOf("/") + 1));
                        //根据源文件，被改动的行，解析涉及的方法和入参
                        list = getMethodOfLines(parsefileToString(file), map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String execCommand(String command, String workDir) throws Exception {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command, null, new File(workDir));
            return execResult(process);
        }catch (Exception e) {
            throw new Exception("目录有误："+workDir);
        }
    }

    private static String execResult(Process process) throws Exception{
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String result = "";
        String line = null;
        while ((line = reader.readLine()) != null) {
            result += (line+"\r\n");
        }
        while ((line = errorReader.readLine()) != null) {
            result += (line+"\r\n");
        }
        int value = process.waitFor();
        if (value != 0){
            throw new Exception("执行外部进程失败："+result);
        }
        return result;
    }

    private static String parsefileToString(File file) {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = null;
        FileReader fileReader = null;
        try {
            int line = 1;
            fileReader = new FileReader(file);
            String temp = null;
            reader = new BufferedReader(fileReader);
            while ((temp = reader.readLine()) != null) {
                content.append(temp).append("\n");
                line++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content.toString();
    }

    private static List<Map> parseDiffLines(String fileDiffs) {
        List<Map> mapList = new ArrayList<Map>();
        try {
            InputStream in = new ByteArrayInputStream(fileDiffs.getBytes("UTF-8"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = "";
            int fromStart = 0;
            int toStart = 0;
            Map map = new HashMap();
            List delLines = new ArrayList();
            List addLines = new ArrayList();
            map.put("delLines", delLines);//删除的所有行号
            map.put("addLines", addLines);//新增的所有行号
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("---")) {
                    if (map.containsKey("fromFileName")) {
                        mapList.add(map);
                        map = new HashMap();
                        delLines = new ArrayList();
                        addLines = new ArrayList();
                        map.put("delLines", delLines);//删除的所有行号
                        map.put("addLines", addLines);//新增的所有行号
                    }
                    //fromfile原文件名
                    map.put("fromFileName", cutDate(line.substring(4)));
                } else if (line.startsWith("+++")) {
                    //tofile
                    map.put("toFileName", cutDate(line.substring(4)));
                } else if (line.startsWith("@@")) {
                    //hunk@@
                    Hunk hunk = parseHunkStart(line);
                    fromStart = hunk.getFromFileRange().getLineStart();
                    toStart = hunk.getToFileRange().getLineStart();
                } else if (line.startsWith("-")) {
                    delLines.add(fromStart);
                    fromStart++;
                } else if (line.startsWith("+")) {
                    addLines.add(toStart);
                    toStart++;
                } else {
                    fromStart++;
                    toStart++;
                }
            }
            mapList.add(map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapList;
    }

    private static String cutDate(String line) {
        Pattern p = Pattern.compile("^(.*)\t[0-9]{4}.*");
        Matcher matcher = p.matcher(line);
        return matcher.matches() ? matcher.group(1) : line;
    }

    private static Hunk parseHunkStart(String currentLine) {
        Pattern pattern = Pattern.compile("^.*-([0-9]+)(?:,([0-9]+))? \\+([0-9]+)(?:,([0-9]+))?.*$");
        Matcher matcher = pattern.matcher(currentLine);
        Hunk hunk = new Hunk();
        if (matcher.matches()) {
            String range1Start = matcher.group(1);
            String range1Count = matcher.group(2) != null ? matcher.group(2) : "1";
            Range fromRange = new Range(Integer.parseInt(range1Start), Integer.parseInt(range1Count));
            String range2Start = matcher.group(3);
            String range2Count = matcher.group(4) != null ? matcher.group(4) : "1";
            Range toRange = new Range(Integer.parseInt(range2Start), Integer.parseInt(range2Count));
            hunk.setFromFileRange(fromRange);
            hunk.setToFileRange(toRange);
        } else {
            throw new IllegalStateException(String.format("No line ranges found in the following hunk start line: \'%s\'. Expected something like \'-1,5 +3,5\'.", new Object[]{currentLine}));
        }
        return hunk;
    }

    private static List<MethodVO> getMethodOfLines(String fileContentTo, Map<String, Object> map) {
        try {
            List<MethodVO> mapList = new ArrayList<MethodVO>();
            mapList.addAll(modifyMethodLines(fileContentTo, (ArrayList<Integer>) map.get("addLines")));
            mapList.addAll(modifyMethodLines(fileContentTo, (ArrayList<Integer>) map.get("delLines")));
            return mapList;
        } catch (IOException e) {
            return null;
        }
    }

    private static List<MethodVO> methodsName(String file) throws IOException {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        InputStream in = new ByteArrayInputStream(file.getBytes());
        CompilationUnit unit = null;
        try {
            unit = JavaParser.parse(in);
        } catch (com.github.javaparser.ParseException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }
        MethodVisitor methodVisitor = new MethodVisitor();
        methodVisitor.visit(unit, null);
        resultMap.put("methods", methodVisitor.getMethodList());
        return methodVisitor.getMethodList();
    }

    private static List<MethodVO> modifyMethodLines(String toFile, ArrayList<Integer> linesList)
            throws IOException {
        List<MethodVO> toMethods = methodsName(toFile);
        return methodSearch(toMethods, linesList);
    }

    //根据行数和方法body涉及的行数获取修改的方法名称和入参
    private static List<MethodVO> methodSearch(List<MethodVO> methods, ArrayList<Integer> linesList) {
        int index = 0;
        int i = 0;
        List<MethodVO> methodList = new ArrayList<MethodVO>();
        //遍历linesList列表
        while (index < linesList.size()) {
            //判断linesList中的参数，如果参数小于第一个方法体的开始行数，直接跳过，不执行后面的操作
            if (linesList.get(index) < methods.get(0).getBeginIndex()) {
                index++;
                continue;
            }
            //遍历方法体的列表
            while (index < linesList.size() && i < methods.size()) {
                //如果修改行数在方法体中
                if (linesList.get(index) <= methods.get(i).getEndIndex() && linesList.get(index) >= methods.get(i).getBeginIndex()) {
                    //继续判断修改行数列表的下一个值，仍然在当前函数体中，就执行下一个参数
                    while (index < linesList.size()) {
                        if (linesList.get(index) <= methods.get(i).getEndIndex()) {
                            index++;
                        } else {
                            break;
                        }
                    }
                    methodList.add(methods.get(i));
                } else {
                    i++;
                }
            }
            index++;
            i = 0;
        }
        return methodList;
    }
}
