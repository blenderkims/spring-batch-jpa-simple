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

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        final Map<String, ExecutionContext> result = new HashMap<>();
        final long totalCount = userRepository.count();
        final int unit = (int)(totalCount / gridSize);
        final String minId = userRepository.findMinId();
        final String maxId = userRepository.findMaxId();
        int number = 0;
        int page = unit;
        String startId = minId;
        String endId = userRepository.findIdsByPageable(pageable(page)).stream().max(Comparator.comparing(String::toString)).orElse(maxId);
        while (startId.compareTo(maxId) < 0) {
            ExecutionContext executionContext = new ExecutionContext();
            result.put("partition" + number, executionContext);
            executionContext.putString("startId", startId);
            executionContext.putString("endId", endId);
            if (maxId.equals(endId)) {
                executionContext.put("isLast", Boolean.TRUE);
            } else {
                executionContext.put("isLast", Boolean.FALSE);
            }
            log.debug("[partition: {}] start id: {}, end id: {}, last: {}", number, startId, endId, executionContext.get("isLast"));
            page = page + unit;
            startId = endId;
            endId = userRepository.findIdsByPageable(pageable(page)).stream().max(Comparator.comparing(String::toString)).orElse(maxId);
            number++;
        }
        return result;
    }

    private PageRequest pageable(int page) {
        return PageRequest.of(page - 1, 1, Sort.by(ASC, "id"));
    }
}
