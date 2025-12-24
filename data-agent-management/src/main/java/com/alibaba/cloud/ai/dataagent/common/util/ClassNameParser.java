package com.alibaba.cloud.ai.dataagent.common.util;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java类名解析器
 * 功能：从Java源代码中提取类名
 */
public class ClassNameParser {

    /**
     * 从Java源代码字符串中解析类名
     */
    public static String parseClassNameFromCode(String javaCode) {
        if (javaCode == null || javaCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Java代码不能为空");
        }

        // 正则表达式匹配类定义
        Pattern pattern = Pattern.compile(
                "\\b(?:public\\s+)?(?:final\\s+)?(?:abstract\\s+)?class\\s+([A-Za-z_][A-Za-z0-9_]*)",
                Pattern.MULTILINE
        );

        Matcher matcher = pattern.matcher(javaCode);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("未找到类定义");
    }

    /**
     * 从Java文件路径解析类名
     */
    public static String parseClassNameFromFile(String filePath) throws IOException {
        Path path = Path.of(filePath);
        String javaCode = Files.readString(path);
        return parseClassNameFromCode(javaCode);
    }

    /**
     * 批量解析目录中的Java文件类名
     */
    public static List<String> parseClassNamesFromDirectory(String directoryPath)
            throws IOException {
        List<String> classNames = new ArrayList<>();

        Files.walk(Path.of(directoryPath))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        String className = parseClassNameFromFile(p.toString());
                        classNames.add(className);
                    } catch (IOException e) {
                        System.err.println("读取文件失败: " + p + ", 错误: " + e.getMessage());
                    }
                });

        return classNames;
    }

    /**
     * 主方法 - 测试类名解析功能
     */
    public static void main(String[] args) {
        try {
            // 测试用例1：标准类定义
            String testCode1 =
                    "public class UserService {\n" +
                            "    private String name;\n" +
                            "    public void setName(String name) {\n" +
                            "        this.name = name;\n" +
                            "    }\n" +
                            "}";

            String className1 = parseClassNameFromCode(testCode1);
            System.out.println("解析到的类名: " + className1);

            // 测试用例2：带修饰符的类
            String testCode2 =
                    "public final class Constants {\n" +
                            "    public static final String APP_NAME = \"类名解析器\";\n" +
                            "}";

            String className2 = parseClassNameFromCode(testCode2);
            System.out.println("解析到的类名: " + className2);

            // 测试用例3：抽象类
            String testCode3 =
                    "public abstract class BaseService {\n" +
                            "    public abstract void execute();\n" +
                            "}";

            // 测试用例4：内部类（不处理）
            String testCode4 =
                    "public class OuterClass {\n" +
                            "    private class InnerClass {\n" +
                            "    }\n" +
                            "}";

            String className3 = parseClassNameFromCode(testCode3);
            System.out.println("解析到的类名: " + className3);

        } catch (Exception e) {
            System.err.println("解析失败: " + e.getMessage());
        }
    }
}
