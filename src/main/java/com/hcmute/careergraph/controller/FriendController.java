package com.hcmute.careergraph.controller;

import com.hcmute.careergraph.dtos.request.user.UserNodeCreationRequest;
import com.hcmute.careergraph.dtos.response.user.UserNodeResponse;
import com.hcmute.careergraph.services.impl.FriendService;
import com.hcmute.careergraph.helper.ApiResponse;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@CrossOrigin(origins = "*")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @PostMapping("/create")
    public ApiResponse<UserNodeResponse> createUserNode(@RequestBody UserNodeCreationRequest request)
            throws BadRequestException {

        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new BadRequestException("Username invalid");
        }

        UserNodeResponse result = friendService.saveUserNode(request.getUsername());
        return ApiResponse.<UserNodeResponse>builder()
                .code(200)
                .message("Completed API create node user")
                .data(result)
                .build();
    }

    @GetMapping("/get-friends")
    public ApiResponse<List<UserNodeResponse>> getFriends(@RequestParam("username") String username,
                                                         @RequestParam("depth") String depth) throws BadRequestException, ChangeSetPersister.NotFoundException {

        if (username == null || depth == null) {
            throw new BadRequestException("Username or depth invalid");
        }

        List<UserNodeResponse> result = friendService.findFriendWithDepth(username, Integer.parseInt(depth));

        return ApiResponse.<List<UserNodeResponse>>builder()
                .code(200)
                .message("Completed API get friend")
                .data(result)
                .build();
    }
}
