package com.alibaba.cloud.ai.dataagent.service.codeJava;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicCodeRunner {

    public static void main(String[] args) {
        String s = executeJavaCode("\n" +
                " class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}");
        System.out.println(s);
    }

    /**
     * 动态编译并执行Java代码
     */
    public static String executeJavaCode(String code) {
        try {
            // 动态编译
            Class<?> compiledClass = compileAndLoadClass(code);

            // 执行main方法
            return invokeMainMethod(compiledClass);
        } catch (Exception e) {
            return "执行错误: " + e.getMessage();
        }
    }

    /**
     * 动态编译并加载类
     */
    private static Class<?> compileAndLoadClass(String code) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("无法获取Java编译器，请确保使用JDK环境");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // 创建内存中的Java文件对象
        JavaFileObject javaFileObject = new JavaSourceFromString("DynamicCode", code);

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, null, null, Arrays.asList(javaFileObject));

        boolean success = task.call();
        if (!success) {
            StringBuilder errorMsg = new StringBuilder("编译错误:\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append(String.format("行%d: %s%n",
                        diagnostic.getLineNumber(), diagnostic.getMessage(null)));
            }
            throw new RuntimeException(errorMsg.toString());
        }

        // 使用自定义类加载器加载类
        MemoryFileManager memoryFileManager = new MemoryFileManager(fileManager);

        try {
            task = compiler.getTask(null, memoryFileManager, null, null, null, Arrays.asList(javaFileObject));
            task.call();

            JavaFileObject compiledFile = memoryFileManager.getJavaFileForOutput(
                    StandardLocation.CLASS_OUTPUT, "DynamicCode", JavaFileObject.Kind.CLASS, null);

            byte[] bytecode = ((MemoryJavaFileObject) compiledFile).getByteCode();

            DynamicClassLoader classLoader = new DynamicClassLoader();
            return classLoader.defineClass("DynamicCode", bytecode);
        } catch (Exception e) {
            throw new RuntimeException("类加载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 反射调用main方法
     */
    private static String invokeMainMethod(Class<?> clazz) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream, true));

        try {
            Method mainMethod = clazz.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);

            return outputStream.toString().trim();
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * 内存中的Java源文件对象
     */
    static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /**
     * 内存文件管理器
     */
    static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final List<MemoryJavaFileObject> compiledClasses = new ArrayList<>();

        MemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            MemoryJavaFileObject fileObject = new MemoryJavaFileObject(className, kind);
            compiledClasses.add(fileObject);
            return fileObject;
        }
    }

    /**
     * 内存中的Java类文件对象
     */
    static class MemoryJavaFileObject extends SimpleJavaFileObject {
        private ByteArrayOutputStream byteCodeStream;

        MemoryJavaFileObject(String name, Kind kind) {
            super(URI.create("mem:///" + name.replace('.', '/') + kind.extension), kind);
        }


        @Override
        public OutputStream openOutputStream() {
            byteCodeStream = new ByteArrayOutputStream();
            return byteCodeStream;
        }

        public byte[] getByteCode() {
            return byteCodeStream != null ? byteCodeStream.toByteArray() : new byte[0];
        }
    }

    /**
     * 动态类加载器
     */
    static class DynamicClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
