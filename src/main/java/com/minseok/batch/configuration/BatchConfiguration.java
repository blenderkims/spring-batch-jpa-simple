package com.minseok.batch.configuration;

import com.minseok.batch.listener.WorkerTaskExecutorShutdownListener;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * package      : com.minseok.batch.configuration
 * class        : BatchConfiguration
 * author       : blenderkims
 * date         : 2023/04/11
 * description  :
 */
@EnableJpaAuditing
@EnableBatchProcessing
@Configuration
public class BatchConfiguration {

    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * Worker task executor thread pool task executor.
     *
     * @return the thread pool task executor
     */
    @Bean("workerTaskExecutor")
    public ThreadPoolTaskExecutor workerTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize((int) (POOL_SIZE * 1.5));
        taskExecutor.setMaxPoolSize(POOL_SIZE * 2);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAllowCoreThreadTimeOut(true);
        taskExecutor.setThreadNamePrefix("worker-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }

    /**
     * Worker task executor shutdown listener job execution listener.
     *
     * @param workerTaskExecutor the worker task executor
     * @return the job execution listener
     */
    @Bean("workerTaskExecutorShutdownListener")
    public JobExecutionListener workerTaskExecutorShutdownListener(
            @Qualifier("workerTaskExecutor") ThreadPoolTaskExecutor workerTaskExecutor) {
        return new WorkerTaskExecutorShutdownListener(workerTaskExecutor);
    }
}
