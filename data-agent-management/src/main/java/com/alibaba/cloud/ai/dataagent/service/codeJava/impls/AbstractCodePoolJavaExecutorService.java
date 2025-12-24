/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.dataagent.service.codeJava.impls;

import com.alibaba.cloud.ai.dataagent.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.dataagent.service.code.CodePoolExecutorService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 运行Java任务的容器池
 *
 */
@Slf4j
public abstract class AbstractCodePoolJavaExecutorService implements CodePoolExecutorService {

	// Thread pool, running temporarily stored tasks
	protected final ExecutorService consumerThreadPool;

	// Configuration properties
	protected final CodeExecutorProperties properties;

	/**
	 * 在指定容器ID的容器运行任务
	 * @param request 任务请求对象
	 * @param className 包名
	 * @return 运行结果对象
	 */
	protected abstract CompletableFuture<TaskResponse> executeJavaCode(String request, String className);


	public AbstractCodePoolJavaExecutorService(CodeExecutorProperties properties) {
		this.properties = properties;

		this.consumerThreadPool = new ThreadPoolExecutor(properties.getCoreThreadSize(), properties.getMaxThreadSize(),
				properties.getKeepThreadAliveTime(), TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(properties.getThreadQueueSize()));
	}

	@Override
	public TaskResponse runTask(TaskRequestJava request) {
		try {
			CompletableFuture<TaskResponse> future = this.executeJavaCode(request.code(), request.className());

			// 等待执行完成（可设置超时）
			return future.get(30, TimeUnit.SECONDS);

		} catch (TimeoutException e) {
			return TaskResponse.exception("执行超时："+e.getMessage());
		} catch (Exception e) {
			return TaskResponse.exception("任务执行异常: " + e.getMessage());
		}
	}

}
