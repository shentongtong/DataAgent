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
import com.alibaba.cloud.ai.dataagent.service.code.CodePoolExecutorService;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.dataagent.common.constant.Constant.*;

/**
 * 根据SQL查询结果生成JAVA代码，并运行JAVA代码获取运行结果。
 *
 * @author stt
 * @since 2025/11/20
 */
@Slf4j
@Component
public class JavaExecuteNode implements NodeAction {

	private final CodePoolExecutorService codePoolExecutor;

	private final ObjectMapper objectMapper;

	public JavaExecuteNode(CodePoolExecutorService codePoolExecutor) {
		this.codePoolExecutor = codePoolExecutor;
		this.objectMapper = JsonUtil.getObjectMapper();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		try {
			// Get context
			String javaCode = StateUtil.getStringValue(state, JAVA_GENERATE_NODE_OUTPUT);
			List<Map<String, String>> sqlResults = StateUtil.getListValue(state, SQL_RESULT_LIST_MEMORY);
			if (null == sqlResults || sqlResults.isEmpty()) {
				String errorMsg = "Java Execute Failed!\n sql执行结果列表为空（JavaExecuteNode-sqlResults）" ;
				log.error(errorMsg);
				throw new RuntimeException(errorMsg);
			}
			CodePoolExecutorService.TaskRequestJava taskRequestJava = new CodePoolExecutorService.TaskRequestJava(javaCode,
					 ClassNameParser.parseClassNameFromCode(javaCode),sqlResults);
			// Run JAVA code
			CodePoolExecutorService.TaskResponse taskResponse = this.codePoolExecutor.runTask(taskRequestJava);
			if (!taskResponse.isSuccess()) {
				String errorMsg = "Java Execute Failed!\nStdOut: " + taskResponse.stdOut() + "\nStdErr: "
						+ taskResponse.stdErr() + "\nExceptionMsg: " + taskResponse.exceptionMsg();
				log.error(errorMsg);
				throw new RuntimeException(errorMsg);
			}

			// Java输出的JSON字符串可能有Unicode转义形式，需要解析回汉字
			String stdout = taskResponse.stdOut();
			try {
				Object value = objectMapper.readValue(stdout, Object.class);
				stdout = objectMapper.writeValueAsString(value);
			}
			catch (Exception e) {
				stdout = taskResponse.stdOut();
			}
			String finalStdout = stdout;

			log.info("Java Execute Success! StdOut: {}", finalStdout);

			// Create display flux for user experience only
			Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
				emitter.next(ChatResponseUtil.createResponse("开始执行Java代码..."));
				emitter.next(ChatResponseUtil.createResponse("标准输出："));
				emitter.next(ChatResponseUtil.createPureResponse(TextType.JSON.getStartSign()));
				emitter.next(ChatResponseUtil.createResponse(finalStdout));
				emitter.next(ChatResponseUtil.createPureResponse(TextType.JSON.getEndSign()));
				emitter.next(ChatResponseUtil.createResponse("Java代码执行成功！"));
				emitter.complete();
			});

			// Create generator using utility class, returning pre-computed business logic
			// result
			Flux<GraphResponse<StreamingOutput>> generator = FluxUtil.createStreamingGeneratorWithMessages(
					this.getClass(), state,
					v -> Map.of(JAVA_EXECUTE_NODE_OUTPUT, finalStdout, JAVA_IS_SUCCESS, true), displayFlux);

			return Map.of(JAVA_EXECUTE_NODE_OUTPUT, generator);
		}
		catch (Exception e) {
			String errorMessage = e.getMessage();
			log.error("Java Execute Exception: {}", errorMessage);

			// Prepare error result
			Map<String, Object> errorResult = Map.of(JAVA_EXECUTE_NODE_OUTPUT, errorMessage, JAVA_IS_SUCCESS,
					false);

			// Create error display flux
			Flux<ChatResponse> errorDisplayFlux = Flux.create(emitter -> {
				emitter.next(ChatResponseUtil.createResponse("开始执行Java代码..."));
				emitter.next(ChatResponseUtil.createResponse("Java代码执行失败: " + errorMessage));
				emitter.complete();
			});

			// Create error generator using utility class
			var generator = FluxUtil.createStreamingGeneratorWithMessages(this.getClass(), state, v -> errorResult,
					errorDisplayFlux);

			return Map.of(JAVA_EXECUTE_NODE_OUTPUT, generator);
		}
	}

}
