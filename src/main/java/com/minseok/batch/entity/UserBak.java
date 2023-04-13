package com.minseok.batch.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * package      : com.minseok.batch.entity
 * class        : UserBak
 * author       : blenderkims
 * date         : 2023/04/12
 * description  :
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "tb_user_bak")
public class UserBak {
    @Id
    @Column(unique = true, nullable = false, length = 32)
    private String id;
    @Column(unique = true, nullable = false, length = 320)
    private String email;
    private String password;
    private String name;
    private String nickname;
    private String mobile;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /**
     * Instantiates a new User bak.
     *
     * @param id         the id
     * @param email      the email
     * @param password   the password
     * @param name       the name
     * @param nickname   the nickname
     * @param mobile     the mobile
     * @param createdAt  the created at
     * @param modifiedAt the modified at
     */
    @Builder
    public UserBak(String id, String email, String password, String name, String nickname, String mobile, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.mobile = mobile;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    /**
     * Of user bak.
     *
     * @param user the user
     * @return the user bak
     */
    public static UserBak of(User user) {
        return UserBak.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .name(user.getName())
                .nickname(user.getNickname())
                .mobile(user.getMobile())
                .createdAt(user.getCreatedAt())
                .modifiedAt(user.getModifiedAt())
                .build();
    }
}
