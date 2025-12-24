package com.alibaba.cloud.ai.dataagent.service.codeJava;

import com.alibaba.cloud.ai.dataagent.service.code.CodePoolExecutorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL执行结果数据分析器
 * 功能：分析SQL查询结果，提供数据统计、质量评估和业务洞察
 */
public class SQLResultAnalyzer {

    /**
     * 分析SQL执行结果数据
     */
    public CodePoolExecutorService.TaskResponse analyzeSQLResults(List<Map<String, String>> sqlResults) {
        try {
            if (sqlResults == null || sqlResults.isEmpty()) {
                return CodePoolExecutorService.TaskResponse.success("SQL执行结果为空，无数据需要分析");
            }

            StringBuilder analysis = new StringBuilder();

            // 1. 数据概览分析
            analysis.append("## 📊 SQL执行结果数据分析报告\n\n");

            // 基础统计信息
            analysis.append("### 📈 基础统计\n");
            analysis.append("- **总记录数**: ").append(sqlResults.size()).append("\n");

            if (!sqlResults.isEmpty()) {
                Map<String, String> firstRow = sqlResults.get(0);
                analysis.append("- **列数**: ").append(firstRow.size()).append("\n");

                // 2. 数据结构分析
                analysis.append("### 🏗️ 数据结构\n");
                analysis.append("- **列名列表**: ").append(String.join(", ", firstRow.keySet())).append("\n");

                // 3. 数据质量评估
                analysis.append("### ✅ 数据质量评估\n");
                analysis.append("- **数据完整性**: ").append(assessDataCompleteness(sqlResults)).append("\n");
                analysis.append("- **数据类型分布**: ").append(analyzeDataTypes(sqlResults)).append("\n");

                // 4. 业务洞察
                analysis.append("### 💡 业务洞察\n");
                analysis.append("- **关键指标**: ").append(extractKeyMetrics(sqlResults)).append("\n");

                // 5. 异常检测
                analysis.append("### 🔍 异常检测\n");
                analysis.append("- **异常记录**: ").append(detectAnomalies(sqlResults)).append("\n");
            }
            return new CodePoolExecutorService.TaskResponse(true, false, analysis.toString(), null, null);

        } catch (Exception e) {
            return CodePoolExecutorService.TaskResponse.exception("SQL结果分析失败: " + e.getMessage());
        }
    }

    /**
     * 评估数据完整性
     */
    private String assessDataCompleteness(List<Map<String, String>> results) {
        int totalCells = results.size() * (results.isEmpty() ? 0 : results.get(0).size());
        int nullCells = 0;

        for (Map<String, String> row : results) {
            for (String value : row.values()) {
                if (value == null || value.trim().isEmpty()) {
                    nullCells++;
                }
            }

            double completenessRate = totalCells > 0 ? (totalCells - nullCells) * 100.0 / totalCells : 0;
            return String.format("%.2f%%", completenessRate);
        }
        return null;
    }

    /**
     * 分析数据类型分布
     */
    private String analyzeDataTypes(List<Map<String, String>> results) {
        Map<String, Integer> typeCount = new HashMap<>();

        for (Map<String, String> row : results) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String value = entry.getValue();
                String type = classifyDataType(value);
                typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
            }
        }

        return typeCount.toString();
    }

    /**
     * 提取关键业务指标
     */
    private String extractKeyMetrics(List<Map<String, String>> results) {
        List<String> metrics = new ArrayList<>();

        if (results.size() > 0) {
            metrics.add("记录数统计");
            metrics.add("字段分布");
        }

        return String.join(", ", metrics);
    }

    /**
     * 检测数据异常
     */
    private String detectAnomalies(List<Map<String, String>> results) {
        int anomalies = 0;

        for (Map<String, String> row : results) {
            for (String value : row.values()) {
                if (value != null && value.matches(".*[^\\w\\s].*")) {
                    anomalies++;
                }
            }
        }
        return anomalies + "条异常记录";
    }

    /**
     * 对数据进行分类
     */
    private String classifyDataType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "空值";
        }

        if (value.matches("\\d+")) {
            return "整数";
        } else if (value.matches("\\d+\\.\\d+")) {
            return "浮点数";
        } else if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "日期";
        } else if (value.matches("true|false")) {
            return "布尔值";
        } else {
            return "字符串";
        }
    }
}
