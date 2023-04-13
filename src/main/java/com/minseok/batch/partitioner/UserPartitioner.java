package com.minseok.batch.partitioner;

import com.minseok.batch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.springframework.data.domain.Sort.Direction.ASC;

/**
 * package      : com.minseok.batch.partitioner
 * class        : UserPartitioner
 * author       : blenderkims
 * date         : 2023/04/12
 * description  :
 */
@Slf4j
@RequiredArgsConstructor
public class UserPartitioner implements Partitioner {

    private final UserRepository userRepository;

    private final Function<Integer, PageRequest> pageFunction = (page) -> {
        return PageRequest.of(page - 1, 1, Sort.by(ASC, "id"));
    };

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        final Map<String, ExecutionContext> result = new HashMap<>();
        final long totalCount = userRepository.count();
        final int unit = (int)(totalCount / gridSize);
        String minId = userRepository.findMinId();
        String maxId = userRepository.findMaxId();
        int number = 0;
        int page = unit;
        String start = minId;
        String end = userRepository.findIdsByPageable(pageFunction.apply(page)).stream().max(Comparator.comparing(String::toString)).orElse(maxId);
        while (start.compareTo(maxId) < 0) {
            log.debug("[partition: {}] min id: {}, max id: {}", number, start, end);
            ExecutionContext executionContext = new ExecutionContext();
            result.put("partition" + number, executionContext);
            executionContext.putString("minId", start);
            executionContext.putString("maxId", end);
            page = page + unit;
            start = end;
            end = userRepository.findIdsByPageable(pageFunction.apply(page)).stream().max(Comparator.comparing(String::toString)).orElse(maxId);
            number++;
        }
        return result;
    }
}
