/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dataagent.workflow.node;

import com.alibaba.cloud.ai.dataagent.common.enums.TextType;
import com.alibaba.cloud.ai.dataagent.common.util.*;
import com.alibaba.cloud.ai.dataagent.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.dataagent.dto.planner.ExecutionStep;
import com.alibaba.cloud.ai.dataagent.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.dataagent.prompt.PromptConstant;
import com.alibaba.cloud.ai.dataagent.service.llm.LlmService;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.dataagent.common.constant.Constant.*;


/**
 * 生成Java代码的节点
 *
 * @author vlsmb
 * @since 2025/7/30
 */
@Slf4j
@Component
public class JavaGenerateNode implements NodeAction {

	private static final int SAMPLE_DATA_NUMBER = 5;

	private static final int MAX_TRIES_COUNT = 5;

	private final ObjectMapper objectMapper;

	private final CodeExecutorProperties codeExecutorProperties;

	private final LlmService llmService;

	public JavaGenerateNode(CodeExecutorProperties codeExecutorProperties, LlmService llmService) {
		this.codeExecutorProperties = codeExecutorProperties;
		this.llmService = llmService;
		this.objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		// Get context
		SchemaDTO schemaDTO = StateUtil.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);
		List<Map<String, String>> sqlResults = StateUtil.hasValue(state, SQL_RESULT_LIST_MEMORY)
				? StateUtil.getListValue(state, SQL_RESULT_LIST_MEMORY) : new ArrayList<>();
		boolean codeRunSuccess = StateUtil.getObjectValue(state, JAVA_IS_SUCCESS, Boolean.class, true);
		int triesCount = StateUtil.getObjectValue(state, JAVA_TRIES_COUNT, Integer.class, MAX_TRIES_COUNT);

		//准备SQL数据文件作为System.in输入
		prepareSQLInputData(Paths.get("D:\\WorkProject\\AI\\DataAgent\\spring-ai-data-agent-java\\src\\main\\resources\\generateJavaFile"), sqlResults);

		String userPrompt = StateUtil.getCanonicalQuery(state);
		if (!codeRunSuccess) {
			// Last generated Java code failed to run, inform AI model of this
			// information
			String lastCode = StateUtil.getStringValue(state, JAVA_GENERATE_NODE_OUTPUT);
			String lastError = StateUtil.getStringValue(state, JAVA_EXECUTE_NODE_OUTPUT);
			userPrompt += String.format("""
					上次尝试生成的Java代码运行失败，请你重新生成符合要求的Java代码。
					【上次生成代码】
					```Java
					%s
					```
					【运行错误信息】
					```
					%s
					```
					""", lastCode, lastError);
		}

		ExecutionStep executionStep = PlanProcessUtil.getCurrentExecutionStep(state);

		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();

		// Load Java code generation template
		String systemPrompt = PromptConstant.getJavaGeneratorPromptTemplate()
			.render(Map.of("java_memory", codeExecutorProperties.getLimitMemory().toString(), "java_timeout",
					codeExecutorProperties.getCodeTimeout(), "database_schema",
					objectMapper.writeValueAsString(schemaDTO), "sample_input",
					objectMapper.writeValueAsString(sqlResults.stream().limit(SAMPLE_DATA_NUMBER).toList()),
					"plan_description", objectMapper.writeValueAsString(toolParameters)));

		Flux<ChatResponse> JavaGenerateFlux = llmService.call(systemPrompt, userPrompt);

		Flux<GraphResponse<StreamingOutput>> generator = FluxUtil.createStreamingGeneratorWithMessages(this.getClass(),
				state, "开始执行Java代码...", "执行完成！", aiResponse -> {
					// Some AI models still output Markdown markup (even though Prompt has
					// emphasized this)
					aiResponse = aiResponse.substring(TextType.JAVA.getStartSign().length(),
							aiResponse.length() - TextType.JAVA.getEndSign().length());
					aiResponse = MarkdownParserUtil.extractRawText(aiResponse);
					log.info("Java Generate Code: {}", aiResponse);
					return Map.of(JAVA_GENERATE_NODE_OUTPUT, aiResponse, JAVA_TRIES_COUNT, triesCount - 1);
				},
				Flux.concat(Flux.just(ChatResponseUtil.createPureResponse(TextType.JAVA.getStartSign())),
						JavaGenerateFlux,
						Flux.just(ChatResponseUtil.createPureResponse(TextType.JAVA.getEndSign()))));

		return Map.of(JAVA_GENERATE_NODE_OUTPUT, generator);
	}

	private static void prepareSQLInputData(Path tempDir, List<Map<String, String>> sqlResults) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		// 构建输入数据结构
		Map<String, Object> inputData = new LinkedHashMap<>();
		inputData.put("sqlResults", sqlResults);
		inputData.put("totalRecords", sqlResults.size());
		// inputData.put("timestamp", System.currentTimeMillis());

		String jsonInput = mapper.writeValueAsString(inputData);

		// 创建输入文件
		Path inputFile = tempDir.resolve("stdin.txt");
		Files.write(inputFile, jsonInput.getBytes());
	}
}
