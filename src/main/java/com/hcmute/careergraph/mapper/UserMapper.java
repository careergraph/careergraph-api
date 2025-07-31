package com.hcmute.careergraph.mapper;

import com.hcmute.careergraph.dtos.request.user.UserCreationRequest;
import com.hcmute.careergraph.dtos.response.user.UserResponse;
import com.hcmute.careergraph.entities.mysql.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "address", ignore = true)
    User toUser(UserCreationRequest request);

    @Mapping(target = "address", ignore = true)
    UserResponse toUserResponse(User user);
}
