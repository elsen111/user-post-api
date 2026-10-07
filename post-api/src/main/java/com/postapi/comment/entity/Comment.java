package com.postapi.comment.entity;

import com.postapi.common.base.BaseEntity;
import com.postapi.post.entity.Post;
import com.postapi.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_post_id", columnList = "post_id"),
                @Index(name = "idx_comments_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_comments_post"
            )
    )
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_comments_user"
            )
    )
    private User user;

    @Column(nullable = false, length = 3000)
    private String content;

}
