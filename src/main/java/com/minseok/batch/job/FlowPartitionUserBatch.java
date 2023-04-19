package com.minseok.batch.job;

import com.minseok.batch.configuration.BatchConfiguration;
import com.minseok.batch.entity.User;
import com.minseok.batch.entity.UserBak;
import com.minseok.batch.partitioner.UserPartitioner;
import com.minseok.batch.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import java.util.Map;

/**
 * package      : com.minseok.batch.job
 * class        : FlowPartitionUserBatch
 * author       : blenderkims
 * date         : 2023/04/13
 * description  :
 */
@Slf4j
@Configuration
public class FlowPartitionUserBatch extends AbstractBatch {
    private static final String JOB_NAME = "flowPartitionUserBatchJob";
    private static final int CHUNK_SIZE = 1000;
    private static final int PAGE_SIZE = 500;
    private static final int RETRY_LIMIT = 3;
    private final TaskExecutor workerTaskExecutor;
    private final JobExecutionListener workerTaskExecutorShutdownListener;
    private final UserRepository userRepository;

    /**
     * Instantiates a new Flow partition user batch.
     *
     * @param jobBuilderFactory                  the job builder factory
     * @param stepBuilderFactory                 the step builder factory
     * @param entityManagerFactory               the entity manager factory
     * @param workerTaskExecutor                 the worker task executor
     * @param workerTaskExecutorShutdownListener the worker task executor shutdown listener
     * @param userRepository                     the user repository
     */
    public FlowPartitionUserBatch(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, EntityManagerFactory entityManagerFactory
            , @Qualifier("workerTaskExecutor") TaskExecutor workerTaskExecutor, @Qualifier("workerTaskExecutorShutdownListener") JobExecutionListener workerTaskExecutorShutdownListener
            , UserRepository userRepository) {
        super(jobBuilderFactory, stepBuilderFactory, entityManagerFactory);
        this.workerTaskExecutor = workerTaskExecutor;
        this.workerTaskExecutorShutdownListener = workerTaskExecutorShutdownListener;
        this.userRepository = userRepository;
    }
    public String jobName() {
        return JOB_NAME;
    }

    /**
     * Partition handler task executor partition handler.
     *
     * @return the task executor partition handler
     */
    @Bean(JOB_NAME + "PartitionHandler")
    public TaskExecutorPartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setTaskExecutor(workerTaskExecutor);
        partitionHandler.setGridSize(BatchConfiguration.POOL_SIZE);
        partitionHandler.setStep(partitionStep());
        return partitionHandler;
    }

    /**
     * Partitioner partitioner.
     *
     * @return the partitioner
     */
    @Bean(JOB_NAME + "Partitioner")
    public Partitioner partitioner() {
        return new UserPartitioner(userRepository);
    }

    @Bean(JOB_NAME)
    public Job batchJob() {
        log.info("[start job] tb_user to tb_user_bak synchronization data");
        return jobBuilderFactory.get(JOB_NAME)
                .start(startStep())
                .next(cleanupStep())
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                .build();
    }
    @Bean(JOB_NAME + "StartStep")
    @JobScope
    public Step startStep() {
        log.info("[start step] tb_user to tb_user_bak merge data");
        return stepBuilderFactory.get(JOB_NAME + "StartStep")
                .partitioner("partitionStep", partitioner())
                .step(partitionStep())
                .partitionHandler(partitionHandler())
                .build();
    }

    /**
     * Partition step step.
     *
     * @return the step
     */
    @Bean(JOB_NAME + "PartitionStep")
    public Step partitionStep() {
        log.info("[partition step] tb_user to tb_user_bak merge data");
        return stepBuilderFactory.get(JOB_NAME + "PartitionStep")
                .<User, UserBak>chunk(CHUNK_SIZE)
                .reader(itemReader(null, null, null))
                .processor(itemProcessor())
                .writer(itemWriter(null, null, null))
                .faultTolerant()
                .retry(OptimisticLockException.class)
                .retryLimit(RETRY_LIMIT)
                .backOffPolicy(new FixedBackOffPolicy())
                .build();
    }

    /**
     * Cleanup step step.
     *
     * @return the step
     */
    @Bean(JOB_NAME + "CleanupStep")
    @JobScope
    public Step cleanupStep() {
        log.debug("[cleanup step] tb_user_bak delete all remain data");
        return stepBuilderFactory.get(JOB_NAME + "CleanupStep")
                .tasklet((contribution, chunkContext) -> {
                    EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                    Query query = entityManager.createNativeQuery("delete ub from tb_user_bak ub where not exists(select * from tb_user u where u.id = ub.id)");
                    query.executeUpdate();
                    entityManager.flush();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * Item reader jpa paging item reader.
     *
     * @param startId the start id
     * @param endId   the end id
     * @param isLast  the is last
     * @return the jpa paging item reader
     */
    @Bean(name = JOB_NAME + "ItemReader", destroyMethod = "close")
    @StepScope
    public JpaPagingItemReader<User> itemReader(
            @Value("#{stepExecutionContext[startId]}") String startId
            , @Value("#{stepExecutionContext[endId]}") String endId
            , @Value("#{stepExecutionContext[isLast]}") Boolean isLast) {
        log.debug("[reader] start id: {}, end id: {}, last: {}", startId, endId, isLast);
        StringBuilder query = new StringBuilder();
        query.append("select u from User u where u.id >= :startId and u.id");
        query.append(isLast ? " <= " : " < ");
        query.append(":endId order by u.id asc");
        return new JpaPagingItemReaderBuilder<User>()
                .name(JOB_NAME + "ItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(PAGE_SIZE)
                .queryString(query.toString())
                .parameterValues(Map.of("startId", startId, "endId", endId))
                .build();
    }

    /**
     * Item processor item processor.
     *
     * @return the item processor
     */
    @Bean(JOB_NAME + "ItemProcessor")
    @StepScope
    public ItemProcessor<User, UserBak> itemProcessor() {
        return  user -> UserBak.of(user);
    }

    /**
     * Item writer jpa item writer.
     *
     * @param startId the start id
     * @param endId   the end id
     * @param isLast  the is last
     * @return the jpa item writer
     */
    @Bean(JOB_NAME + "ItemWriter")
    @StepScope
    public JpaItemWriter<UserBak> itemWriter(
            @Value("#{stepExecutionContext[startId]}") String startId
            , @Value("#{stepExecutionContext[endId]}") String endId
            , @Value("#{stepExecutionContext[isLast]}") Boolean isLast) {
        log.debug("[writer] start id: {}, end id: {}, last: {}", startId, endId, isLast);
        return new JpaItemWriterBuilder<UserBak>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }


}
