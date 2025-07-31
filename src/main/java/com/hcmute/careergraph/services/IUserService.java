package com.hcmute.careergraph.services;


import com.hcmute.careergraph.dtos.request.user.UserCreationRequest;
import com.hcmute.careergraph.dtos.response.user.UserResponse;
import com.hcmute.careergraph.helper.PageResponse;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.util.List;

public interface IUserService {

    List<UserResponse> findAllUsers()
            throws ChangeSetPersister.NotFoundException;

    UserResponse createUser(UserCreationRequest request)
            throws ChangeSetPersister.NotFoundException;

    UserResponse findInfoDetail()
            throws ChangeSetPersister.NotFoundException;

    PageResponse<UserResponse> findAllUsers(int page, int size) throws ChangeSetPersister.NotFoundException;
}
