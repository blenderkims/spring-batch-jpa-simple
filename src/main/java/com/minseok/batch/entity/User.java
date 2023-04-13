package com.minseok.batch.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * package      : com.minseok.batch.entity
 * class        : User
 * author       : blenderkims
 * date         : 2023/04/11
 * description  :
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "tb_user")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(unique = true, nullable = false, length = 32)
    private String id;
    @Column(unique = true, nullable = false, length = 320)
    private String email;
    private String password;
    private String name;
    private String nickname;
    private String mobile;
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    /**
     * Instantiates a new User.
     *
     * @param email    the email
     * @param password the password
     * @param name     the name
     * @param nickname the nickname
     * @param mobile   the mobile
     */
    @Builder
    public User(String email, String password, String name, String nickname, String mobile) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.mobile = mobile;
    }
}
