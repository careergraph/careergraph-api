package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.dtos.response.user.UserNodeResponse;
import com.hcmute.careergraph.entities.graph.User;
import com.hcmute.careergraph.repository.graph.UserNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FriendService {

    @Autowired
    private UserNodeRepository userNodeRepository;

    public List<UserNodeResponse> findFriendWithDepth(String username, int depth) throws ChangeSetPersister.NotFoundException {

        List<User> users = userNodeRepository.findAllFriendsWithDepth(username, depth);
        if (users == null) {
            throw new ChangeSetPersister.NotFoundException();
        }

        List<UserNodeResponse> userNodeList = new ArrayList<>();
        users.forEach(tmp -> {
            UserNodeResponse userNode = UserNodeResponse.builder()
                    .id(tmp.getId())
                    .username(tmp.getUsername())
                    .build();
            userNodeList.add(userNode);
        });
        return userNodeList;
    }

    public UserNodeResponse saveUserNode(String label) {

        User userNode = User.builder()
                .username(label)
                .build();

        User newUserNode = userNodeRepository.save(userNode);
        return UserNodeResponse.builder()
                .id(newUserNode.getId())
                .username(newUserNode.getUsername())
                .build();
    }
}
