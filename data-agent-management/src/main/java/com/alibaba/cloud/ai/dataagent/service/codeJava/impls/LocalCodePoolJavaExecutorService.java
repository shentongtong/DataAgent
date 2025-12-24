
package com.alibaba.cloud.ai.dataagent.service.codeJava.impls;

import com.alibaba.cloud.ai.dataagent.common.util.ClassNameParser;
import com.alibaba.cloud.ai.dataagent.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.dataagent.service.code.CodePoolExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java代码执行器 - 支持动态编译和执行Java代码
 * 提供安全隔离的代码执行环境
 */
@Slf4j
public class LocalCodePoolJavaExecutorService extends AbstractCodePoolJavaExecutorService implements CodePoolExecutorService {

    public LocalCodePoolJavaExecutorService(CodeExecutorProperties properties) {
        super(properties);
    }

    public static void main(String[] args) {
        LocalCodePoolJavaExecutorService executor = new LocalCodePoolJavaExecutorService(new CodeExecutorProperties());

        // 测试代码 - 包含SQL数据访问逻辑
        String testCode =
                "package com.run.generateJavaFile;\n" +
                        "\n" +
                        "import java.io.*;\n" +
                        "import java.util.*;\n" +
                        "import com.google.gson.Gson;\n" +
                        "import com.google.gson.reflect.TypeToken;\n" +
                        "\n" +
                        "public class GenerateJavaFile {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        try {\n" +
                        "            // 从文件读取JSON数据\n" +
                        "            BufferedReader reader = new BufferedReader(new FileReader(\"D:\\\\WorkProject\\\\AI\\\\DataAgent\\\\spring-ai-data-agent-java\\\\src\\\\main\\\\resources\\\\generateJavaFile\\\\stdin.txt\"));\n" +
                        "            StringBuilder jsonData = new StringBuilder();\n" +
                        "            String line;\n" +
                        "            while ((line = reader.readLine()) != null) {\n" +
                        "                jsonData.append(line);\n" +
                        "            }\n" +
                        "\n" +
                        "            // 解析JSON数据\n" +
                        "            Gson gson = new Gson();\n" +
                        "            DataAnalysis input = gson.fromJson(jsonData.toString(), DataAnalysis.class);\n" +
                        "\n" +
                        "            // 获取SQL结果\n" +
                        "            List<Map<String, String>> sqlResults = input.getSqlResults();\n" +
                        "\n" +
                        "            // 过滤2025年的数据\n" +
                        "            List<Map<String, String>> filteredResults = sqlResults.stream()\n" +
                        "                    .filter(map -> \"2025\".equals(map.get(\"year\")))\n" +
                        "                    .collect(Collectors.toList());\n" +
                        "\n" +
                        "            // 计算每个采购单位的合同金额和降本金额\n" +
                        "            Map<String, Map<String, Double>> summary = new HashMap<>();\n" +
                        "            for (Map<String, String> record : filteredResults) {\n" +
                        "                String businessDemandUnit = record.get(\"business_demand_unit\");\n" +
                        "                double contractAmountWan = Double.parseDouble(record.get(\"contract_amount_wan\"));\n" +
                        "                double costReductionWan = Double.parseDouble(record.get(\"cost_reduction_wan\"));\n" +
                        "\n" +
                        "                if (!summary.containsKey(businessDemandUnit)) {\n" +
                        "                    summary.put(businessDemandUnit, new HashMap<>());\n" +
                        "                    summary.get(businessDemandUnit).put(\"contract_amount_wan\", 0.0);\n" +
                        "                    summary.get(businessDemandUnit).put(\"cost_reduction_wan\", 0.0);\n" +
                        "                }\n" +
                        "\n" +
                        "                summary.get(businessDemandUnit).put(\"contract_amount_wan\", \n" +
                        "                    summary.get(businessDemandUnit).get(\"contract_amount_wan\") + contractAmountWan);\n" +
                        "                summary.get(businessDemandUnit).put(\"cost_reduction_wan\", \n" +
                        "                    summary.get(businessDemandUnit).get(\"cost_reduction_wan\") + costReductionWan);\n" +
                        "            }\n" +
                        "\n" +
                        "            // 构建最终结果\n" +
                        "            Map<String, Object> result = new HashMap<>();\n" +
                        "            result.put(\"summary\", summary);\n" +
                        "\n" +
                        "            // 输出结果为JSON对象\n" +
                        "            System.out.println(gson.toJson(result));\n" +
                        "\n" +
                        "        } catch (Exception e) {\n" +
                        "            // 捕获异常并输出堆栈信息到标准错误\n" +
                        "            e.printStackTrace();\n" +
                        "            System.exit(1);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    static class DataAnalysis {\n" +
                        "        private List<Map<String, String>> sqlResults;\n" +
                        "        private String totalRecords;\n" +
                        "\n" +
                        "        public List<Map<String, String>> getSqlResults() {\n" +
                        "            return sqlResults;\n" +
                        "        }\n" +
                        "\n" +
                        "        public void setSqlResults(List<Map<String, String>> sqlResults) {\n" +
                        "            this.sqlResults = sqlResults;\n" +
                        "        }\n" +
                        "\n" +
                        "        public String getTotalRecords() {\n" +
                        "            return totalRecords;\n" +
                        "        }\n" +
                        "\n" +
                        "        public void setTotalRecords(String totalRecords) {\n" +
                        "            this.totalRecords = totalRecords;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n";


        executor.executeJavaCode(testCode, ClassNameParser.parseClassNameFromCode(testCode))
                .thenAccept(response -> {
                    System.out.println("任务执行状态: " + (response.isSuccess() ? "成功" : "失败"));
                    if(response.isSuccess()) {
                        System.out.println("输出结果:\n" + response.stdOut());
                    }else {
                        System.out.println("异常信息:\n" + response.exceptionMsg());
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("执行异常: " + throwable.getMessage());
                    return null;
                });
        // 等待异步执行完成
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 支持类路径依赖的执行
     */
    public CompletableFuture<TaskResponse> executeJavaCode(String javaCode,
                                                           String className) {
        return CompletableFuture.supplyAsync(() -> {
            Path tempDir = null;
            try {
                tempDir = createTempDirectory();

                // 1. 分析Java代码中的依赖
                Map<String, Object> dependencies = analyzeJavaDependencies(javaCode);
                List<Map<String, String>> mavenDependencies = (List<Map<String, String>>) dependencies.get("maven_dependencies");

                // 2. 下载和管理依赖
                if (mavenDependencies != null && !mavenDependencies.isEmpty()) {
                    manageDependencies(tempDir, mavenDependencies,className);
                }

                // 3. 创建Java源文件
                Path javaFile = createJavaSourceFile(tempDir, javaCode, className);

                // 4. 编译代码
                boolean compileSuccess =compileAndRunClass(className);
                // boolean compileSuccess = compileSingleClass(className);
                if (!compileSuccess) {
                    return TaskResponse.exception("代码编译失败");
                }

                // 5. 执行代码
                String output = runSpecificClass(className);

                //6. 清理文件
                // cleanupSingleFile(javaFile);

                return TaskResponse.success(output);
            } catch (Exception e) {
                return TaskResponse.exception("执行失败：" +e.getLocalizedMessage());
            }
        });
    }



    /**
     * 创建临时工作目录 - 支持指定基础目录
     */
    protected Path createTempDirectory() throws IOException {
        // 直接在此处设置自定义目录路径
        String customBaseDir = "D:\\WorkProject\\AI\\DataAgent\\spring-ai-data-agent-java\\src\\main\\java\\com\\run\\generateJavaFile";
        Path basePath = Paths.get(customBaseDir);

        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }

        return basePath;
    }

    /**
     * 生成Java源文件
     */
    protected Path createJavaSourceFile(Path tempDir, String code, String className)
            throws IOException {
        Path javaFile = tempDir.resolve(className + ".java");

        /*try (BufferedWriter writer = Files.newBufferedWriter(javaFile, StandardCharsets.UTF_8)) {
            writer.write(code);
        }*/
        try (BufferedWriter writer = Files.newBufferedWriter(javaFile)) {
            writer.write(code);
        }
        return javaFile;
    }

    private final String mavenAddress = "D:\\Work Program Files\\maven\\apache-maven-3.6.3\\bin\\mvn.cmd";
    private final String pomAddress = "D:\\WorkProject\\AI\\DataAgent\\spring-ai-data-agent-java\\pom.xml";

    public boolean compileAndRunClass(String className) {
        try {
            // 构建Maven编译命令
            ProcessBuilder compileProcess = new ProcessBuilder(
                    mavenAddress,
                    "clean",
                    "compile",
                    "-f",pomAddress,
                    // "-Dfile.encoding=UTF-8", // 指定字符编码
                    "-q"  // 安静模式，减少输出
            );
            // compileProcess.directory(projectDir);
            // compileProcess.directory(new File("spring-ai-data-agent-java"));

            System.out.println("开始编译项目...");
            Process compile = compileProcess.start();
            int compileExitCode = compile.waitFor();

            if (compileExitCode == 0) {
                System.out.println("编译成功！");
                // runSpecificClass(className);
            } else {
                System.err.println("编译失败，退出码: " + compileExitCode);
                // 读取错误信息
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(compile.getErrorStream())
                );
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println(errorLine);
                }
            }
            return compileExitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String runSpecificClass(String className) throws Exception{
        try {

            // 使用exec-maven-plugin运行特定类
            ProcessBuilder runProcess = new ProcessBuilder(
                    mavenAddress,
                    "-f",pomAddress,
                    "exec:java",
                   // "-Dfile.encoding=UTF-8", // 指定字符编码
                 //    "-Dexec.mainClass=com.run.generateJavaFile." + className,
                    "-q"
            );
            // runProcess.directory(projectDir);
            runProcess.directory(new File("spring-ai-data-agent-java"));


            System.out.println("执行类: " + className);
            Process run = runProcess.start();

            // 读取标准输出
            String outputLine = readStream(run.getInputStream());
            String error = readStream(run.getErrorStream());

            int runExitCode = run.waitFor();

            if (runExitCode != 0 && !error.isEmpty()) {
                throw new Exception("执行错误: " + error);
            }

            return outputLine;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new Exception("执行错误: " + e.getMessage());
        }
    }

    public boolean compileSingleClass(String className) {
        try {

            // 直接编译并运行Calculator类
            ProcessBuilder processBuilder = new ProcessBuilder(
                    mavenAddress,
                    "exec:java",
                    "-Dexec.mainClass=com.run.generateJavaFile."+className
            );
            // processBuilder.directory(projectDir);
            processBuilder.directory(new File("spring-ai-data-agent-java"));


            System.out.println("=== 使用ProcessBuilder编译和执行类 ===");
            Process process = processBuilder.start();

            // 处理输出流
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("程序输出: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("错误输出: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();
            outputThread.join();
            errorThread.join();

            System.out.println("执行完成，退出码: " + exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 使用指定编码读取流内容
     */
    private String readStreamWithEncoding(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }


    /**
     * 清理临时文件
     */
    public void cleanupSingleFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("文件已删除: " + filePath);
            } else {
                System.out.println("文件不存在: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("删除失败: " + filePath);
            e.printStackTrace();
        }
    }


    private static void manageDependencies(Path tempDir,List<Map<String, String>> mavenDeps,String className) throws Exception {
        if (mavenDeps == null || mavenDeps.isEmpty()) {
            return;
        }

        // 创建Maven项目结构
        createMavenProject(tempDir, mavenDeps,className);
    }


    private static Map<String, Object> analyzeJavaDependencies(String javaCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> detectedImports = new LinkedHashSet<>();
        Set<String> packageNames = new LinkedHashSet<>();

        // 分析import语句
        analyzeImports(javaCode, detectedImports, packageNames);

        // 构建Maven依赖配置
        Set<Map<String, String>> mavenDeps = buildMavenDependencies(detectedImports);

        result.put("detected_imports", new ArrayList<>(detectedImports));
        result.put("package_names", new ArrayList<>(packageNames));
        result.put("maven_dependencies",  new ArrayList<>(mavenDeps));
        result.put("dependency_count", mavenDeps.size());

        return result;
    }

    private static void analyzeImports(String javaCode, Set<String> dependencies, Set<String> packages) {
        Pattern importPattern = Pattern.compile("^import\\s+(?:static\\s+)?([^;]+);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(javaCode);

        while (matcher.find()) {
            String importPath = matcher.group(1);
            dependencies.add(importPath);

            // 提取包名
            if (importPath.contains(".")) {
                String packageName = importPath.substring(0, importPath.lastIndexOf("."));
                packages.add(packageName);
            }
        }
    }


    private static Set<Map<String, String>> buildMavenDependencies(Set<String> detectedDeps) {
        Set<Map<String, String>> mavenDeps = new HashSet<>();

        // 常见Java库的Maven依赖映射
        Map<String, Map<String, String>> frameworkMapping = new HashMap<>();

/*
        // fastjson依赖
        Map<String, String> fastjson = new LinkedHashMap<>();
        fastjson.put("group_id", "com.alibaba");
        fastjson.put("artifact_id", "fastjson");
        fastjson.put("version", "2.0.43");
        fastjson.put("scope", "system");
        fastjson.put("systemPath", "${project.basedir}\\src\\main\\java\\com\\alibaba\\cloud\\ai\\dataagent\\generateJavaFile\\fastjson-2.0.43.jar");
        frameworkMapping.put("com.alibaba", fastjson);
*/

        // gson依赖
        Map<String, String> gson = new LinkedHashMap<>();
        gson.put("group_id", "com.google.code.gson");
        gson.put("artifact_id", "gson");
        gson.put("version", "2.8.6");
        frameworkMapping.put("gson", gson);

        // 根据检测到的包名匹配Maven依赖
        for (String detectedDep : detectedDeps) {
            for (Map.Entry<String, Map<String, String>> entry : frameworkMapping.entrySet()) {
                if (detectedDep.contains(entry.getKey())) {
                    mavenDeps.add(entry.getValue());
                }
            }
        }
        return mavenDeps;
    }

    private static void createMavenProject(Path tempDir, List<Map<String, String>> mavenDeps,String className) throws IOException {
        // 创建pom.xml
        String pomContent = generatePomXml(mavenDeps,className);
        Files.write(Path.of("D:\\WorkProject\\AI\\DataAgent\\spring-ai-data-agent-java\\pom.xml"), pomContent.getBytes());
    }


    private static String generatePomXml(List<Map<String, String>> mavenDeps,String className) {
        StringBuilder pomBuilder = new StringBuilder();
        pomBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        pomBuilder.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        pomBuilder.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        pomBuilder.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        pomBuilder.append("    <modelVersion>4.0.0</modelVersion>\n");
        pomBuilder.append("    <groupId>com.run.generateJavaFile</groupId>\n");
        pomBuilder.append("    <artifactId>spring-ai-data-agent-java</artifactId>\n");
        pomBuilder.append("    <version>1.0.0</version>\n");
        pomBuilder.append("    <packaging>jar</packaging>\n");
        pomBuilder.append("    <properties>\n");
        // pomBuilder.append("        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>\n");
        // pomBuilder.append("       <maven.compiler.encoding>UTF-8</maven.compiler.encoding>\n");
        // pomBuilder.append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
        pomBuilder.append("       <maven.compiler.plugin.version>3.11.0</maven.compiler.plugin.version>\n");
        pomBuilder.append("       <exec.plugin.version>3.1.0</exec.plugin.version>\n");
        pomBuilder.append("    </properties>\n");
        pomBuilder.append("    <dependencies>\n");

        for (Map<String, String> dep : mavenDeps) {
            pomBuilder.append("        <dependency>\n");
            pomBuilder.append("            <groupId>").append(dep.get("group_id")).append("</groupId>\n");
            pomBuilder.append("            <artifactId>").append(dep.get("artifact_id")).append("</artifactId>\n");
            pomBuilder.append("            <version>").append(dep.get("version")).append("</version>\n");
            if(StringUtils.isNotEmpty(dep.get("systemPath"))) {
                pomBuilder.append("            <systemPath>").append(dep.get("systemPath")).append("</systemPath>\n");
                pomBuilder.append("            <scope>").append(dep.get("scope")).append("</scope>\n");
            }
            pomBuilder.append("        </dependency>\n");
        }

        pomBuilder.append("    </dependencies>\n");
        pomBuilder.append("    <build>\n");
        pomBuilder.append("        <plugins>\n");
        pomBuilder.append("            <plugin>\n");
        pomBuilder.append("                <groupId>org.apache.maven.plugins</groupId>\n");
        pomBuilder.append("                <artifactId>maven-compiler-plugin</artifactId>\n");
        pomBuilder.append("                <version>${maven.compiler.plugin.version}</version>\n");
        pomBuilder.append("                <configuration>\n");
        // pomBuilder.append("                    <release>17</release>\n");
        pomBuilder.append("                    <source>8</source>\n");
        pomBuilder.append("                    <target>8</target>\n");
        // pomBuilder.append("                    <encoding>UTF-8</encoding>\n");
        pomBuilder.append("                </configuration>\n");
        pomBuilder.append("            </plugin>\n");

        pomBuilder.append("            <plugin>\n");
        pomBuilder.append("                <groupId>org.codehaus.mojo</groupId>\n");
        pomBuilder.append("                <artifactId>exec-maven-plugin</artifactId>\n");
        pomBuilder.append("                <version>3.1.0</version>\n");
        pomBuilder.append("                <configuration>\n");
        pomBuilder.append("                    <mainClass>com.run.generateJavaFile.").append(className).append("</mainClass>\n");
        pomBuilder.append("                </configuration>\n");
        pomBuilder.append("            </plugin>\n");

        pomBuilder.append("            <plugin>\n");
        pomBuilder.append("                <groupId>org.springframework.boot</groupId>\n");
        pomBuilder.append("                <artifactId>spring-boot-maven-plugin</artifactId>\n");
        pomBuilder.append("                <version>3.0.0</version>\n");
        pomBuilder.append("                <configuration>\n");
        pomBuilder.append("                    <includeSystemScope>true</includeSystemScope>\n");
        pomBuilder.append("                </configuration>\n");
        pomBuilder.append("            </plugin>\n");
        pomBuilder.append("        </plugins>\n");

        // pomBuilder.append("        <resources>\n");
        // pomBuilder.append("            <resource>\n");
        // pomBuilder.append("                <directory>lib</directory>\n");
        // pomBuilder.append("                <targetPath>/BOOT-INF/lib/</targetPath>\n");
        // pomBuilder.append("                <includes>\n");
        // pomBuilder.append("                     <include>**/*.jar</include>\n");
        // pomBuilder.append("                </includes>\n");
        // pomBuilder.append("             </resource>\n");
        // pomBuilder.append("         </resources>\n");

        pomBuilder.append("    </build>\n");
        pomBuilder.append("</project>\n");

        return pomBuilder.toString();
    }


    @Override
    public TaskResponse runTask(TaskRequest request) {
        return null;
    }
}
