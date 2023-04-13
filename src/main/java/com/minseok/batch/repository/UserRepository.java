package com.minseok.batch.repository;

import com.minseok.batch.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * package      : com.minseok.batch.repository
 * class        : UserRepository
 * author       : blenderkims
 * date         : 2023/04/12
 * description  :
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find min id string.
     *
     * @return the string
     */
    @Query("select min(u.id) from User u")
    String findMinId();

    /**
     * Find max id string.
     *
     * @return the string
     */
    @Query("select max(u.id) from User u")
    String findMaxId();

    /**
     * Find ids by pageable list.
     *
     * @param pageable the pageable
     * @return the list
     */
    @Query("select u.id from User u")
    List<String> findIdsByPageable(Pageable pageable);
}