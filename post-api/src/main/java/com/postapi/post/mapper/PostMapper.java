package com.postapi.post.mapper;

import com.postapi.post.dto.PostResponse;
import com.postapi.post.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(
            target = "userId",
            source = "user.id"
    )
    @Mapping(
            target = "username",
            source = "user.username"
    )
    PostResponse toResponse(Post post);
}