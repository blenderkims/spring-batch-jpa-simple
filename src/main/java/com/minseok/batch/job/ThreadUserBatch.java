package com.minseok.batch.job;

import com.minseok.batch.configuration.BatchConfiguration;
import com.minseok.batch.entity.User;
import com.minseok.batch.entity.UserBak;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;

import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/**
 * package      : com.minseok.batch.job
 * class        : ThreadUserBatch
 * author       : blenderkims
 * date         : 2023/04/12
 * description  :
 */
@Slf4j
@Configuration
public class ThreadUserBatch extends AbstractBatch {
    private static final String JOB_NAME = "threadUserBatchJob";
    private static final int CHUNK_SIZE = 1000;
    private static final int PAGE_SIZE = 500;
    private final TaskExecutor workerTaskExecutor;
    private final JobExecutionListener workerTaskExecutorShutdownListener;
    @Value("${spring.batch.write.file-path}")
    private String filePath;

    /**
     * Instantiates a new Thread user batch.
     *
     * @param jobBuilderFactory                  the job builder factory
     * @param stepBuilderFactory                 the step builder factory
     * @param entityManagerFactory               the entity manager factory
     * @param workerTaskExecutor                 the worker task executor
     * @param workerTaskExecutorShutdownListener the worker task executor shutdown listener
     */
    public ThreadUserBatch(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, EntityManagerFactory entityManagerFactory
            , @Qualifier("workerTaskExecutor") TaskExecutor workerTaskExecutor, @Qualifier("workerTaskExecutorShutdownListener") JobExecutionListener workerTaskExecutorShutdownListener) {
        super(jobBuilderFactory, stepBuilderFactory, entityManagerFactory);
        this.workerTaskExecutor = workerTaskExecutor;
        this.workerTaskExecutorShutdownListener = workerTaskExecutorShutdownListener;
    }
    public String jobName() {
        return JOB_NAME;
    }
    @Bean(JOB_NAME)
    public Job batchJob() {
        log.debug("[start job] tb_user to csv export");
        return jobBuilderFactory.get(JOB_NAME)
                .start(startStep())
                .incrementer(new RunIdIncrementer())
                .listener(workerTaskExecutorShutdownListener)
                .preventRestart()
                .build();
    }
    @Bean(JOB_NAME + "StartStep")
    public Step startStep() {
        log.debug("[start step] tb_user to csv export");
        return stepBuilderFactory.get(JOB_NAME + "StartStep")
                .<User, UserBak>chunk(CHUNK_SIZE)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .taskExecutor(workerTaskExecutor)
                .throttleLimit(BatchConfiguration.POOL_SIZE)
                .build();
    }

    /**
     * Item reader jpa paging item reader.
     *
     * @return the jpa paging item reader
     */
    @Bean(name = JOB_NAME + "ItemReader", destroyMethod = "close")
    public JpaPagingItemReader<User> itemReader() {
        log.debug("[reader] jpa paging read");
        return new JpaPagingItemReaderBuilder<User>()
                .name(JOB_NAME + "ItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(PAGE_SIZE)
                .queryString("select u from User u order by u.id")
                .build();
    }

    /**
     * Item processor item processor.
     *
     * @return the item processor
     */
    @Bean(JOB_NAME + "ItemProcessor")
    public ItemProcessor<User, UserBak> itemProcessor() {
        return  user -> UserBak.of(user);
    }

    /**
     * Item writer flat file item writer.
     *
     * @return the flat file item writer
     */
    @Bean(JOB_NAME + "ItemWriter")
    public FlatFileItemWriter<UserBak> itemWriter() {
        log.debug("[writer] flat file writer (csv export)");
        final BeanWrapperFieldExtractor<UserBak> extractor = new BeanWrapperFieldExtractor<>();
        final String[] names = FieldUtils.getAllFieldsList(UserBak.class).stream().map(Field::getName).toArray(String[]::new);
        final DelimitedLineAggregator<UserBak> lineAggregator = new DelimitedLineAggregator<>();
        extractor.setNames(names);
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(extractor);
        return new FlatFileItemWriterBuilder<UserBak>()
                .name(JOB_NAME + "ItemWriter")
                .encoding(StandardCharsets.UTF_8.name())
                .resource(new FileSystemResource(filePath + File.separator + "thread_user_batch.csv"))
                .lineAggregator(lineAggregator)
                .headerCallback(writer -> writer.write(StringUtils.joinWith(",", names)))
                .build();
    }

}
