package com.minseok.batch.job;

/**
 * package      : com.minseok.batch.service
 * class        : SimpleUserBatchService
 * author       : blenderkims
 * date         : 2023/04/11
 * description  :
 */

import com.minseok.batch.entity.User;
import com.minseok.batch.entity.UserBak;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

/**
 * The type Simple user batch.
 */
@Slf4j
@Configuration
public class SimpleUserBatch extends AbstractBatch {
    private static final String JOB_NAME = "simpleUserBatchJob";
    private static final int CHUNK_SIZE = 1000;
    private static final int PAGE_SIZE = 500;

    /**
     * Instantiates a new Simple user batch.
     *
     * @param jobBuilderFactory    the job builder factory
     * @param stepBuilderFactory   the step builder factory
     * @param entityManagerFactory the entity manager factory
     */
    public SimpleUserBatch(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, EntityManagerFactory entityManagerFactory) {
        super(jobBuilderFactory, stepBuilderFactory, entityManagerFactory);
    }

    @Override
    public String jobName() {
        return JOB_NAME;
    }

    @Bean(JOB_NAME)
    public Job batchJob() {
        log.debug("[start job] tb_user to tb_user_bak print");
        return jobBuilderFactory.get(JOB_NAME)
                .start(startStep())
                .incrementer(new RunIdIncrementer())
                .preventRestart()
                .build();
    }

    @Bean(JOB_NAME + "StartStep")
    public Step startStep() {
        log.debug("[start step] tb_user to tb_user_bak print");
        return stepBuilderFactory.get(JOB_NAME + "StartStep")
                .<User, UserBak>chunk(CHUNK_SIZE)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
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
     * Item writer item writer.
     *
     * @return the item writer
     */
    @Bean(JOB_NAME + "ItemWriter")
    public ItemWriter<UserBak> itemWriter() {
        log.debug("[writer] tb_user_bak print");
        return items -> {
            items.forEach(i -> log.info("item: {}", i));
        };
    }
}
