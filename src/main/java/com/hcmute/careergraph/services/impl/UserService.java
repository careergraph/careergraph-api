package com.hcmute.careergraph.services.impl;

import com.hcmute.careergraph.dtos.request.user.UserCreationRequest;
import com.hcmute.careergraph.dtos.response.address.AddressResponse;
import com.hcmute.careergraph.dtos.response.user.UserResponse;
import com.hcmute.careergraph.entities.mysql.Address;
import com.hcmute.careergraph.entities.mysql.User;
import com.hcmute.careergraph.enums.EErrorCode;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.helper.PageResponse;
import com.hcmute.careergraph.mapper.AddressMapper;
import com.hcmute.careergraph.mapper.UserMapper;
import com.hcmute.careergraph.repository.graph.UserNodeRepository;
import com.hcmute.careergraph.repository.sql.AddressRepository;
import com.hcmute.careergraph.repository.sql.UserRepository;
import com.hcmute.careergraph.services.IRedisService;
import com.hcmute.careergraph.services.IUserService;
import com.hcmute.careergraph.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IRedisService redisService;

    @Override
    public UserResponse createUser(UserCreationRequest request) throws ChangeSetPersister.NotFoundException {

        boolean existedUsername = userRepository.existsByUsername(request.getUsername());
        boolean existedEmail = userRepository.existsByEmail(request.getEmail());

        if (existedEmail || existedUsername) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Resource already exists"
            );
        }

        User newUser = userMapper.toUser(request);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));

        if (!addressRepository.existsByLocationIgnoreCase(request.getCountry(),
                request.getCity(), request.getDistrict(), request.getSpecial())) {

            Address newAddress = Address.builder()
                    .country(request.getCountry())
                    .city(request.getCity())
                    .district(request.getDistrict())
                    .special(request.getSpecial())
                    .build();
            newUser.setAddress(addressRepository.save(newAddress));
        } else {
            Address existedAddress = addressRepository.findByCountryAndCityAndDistrictAndSpecial(request.getCountry(),
                            request.getCity(),
                            request.getDistrict(),
                            request.getSpecial())
                    .orElseThrow(() -> new ChangeSetPersister.NotFoundException());
            newUser.setAddress(existedAddress);
        }

        try {
            userRepository.save(newUser);

            // Save in Graph
            // ...

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(EErrorCode.UNSAVED_DATA);
        }

        UserResponse savedUser = userMapper.toUserResponse(newUser);
        savedUser.setAddress(AddressResponse.builder()
                        .country(newUser.getAddress().getCountry())
                        .city(newUser.getAddress().getCity())
                        .district(newUser.getAddress().getDistrict())
                        .special(newUser.getAddress().getSpecial())
                .build());
        savedUser.setAge(newUser.getAge()); // Transient

        return savedUser;
    }

    @Override
    public UserResponse findInfoDetail() throws ChangeSetPersister.NotFoundException {

        var context = SecurityContextHolder.getContext();

        String username = context.getAuthentication().getName();
        if (username == null || username.isEmpty()) {
            throw new AppException(EErrorCode.UNAUTHORIZED);
        }

        // Cache hit
        String cacheKey = "user:" + username;

        UserResponse cachedResult = redisService.getObject(cacheKey, UserResponse.class);
        if (cachedResult != null) {
            log.info("Cache hit: {}", username);
            return cachedResult;
        }


        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ChangeSetPersister.NotFoundException());

        UserResponse result = userMapper.toUserResponse(user);
        result.setAddress(AddressResponse.builder()
                .country(user.getAddress().getCountry())
                .city(user.getAddress().getCity())
                .district(user.getAddress().getDistrict())
                .special(user.getAddress().getSpecial())
                .build());

        // Save cache (30 minutes)
        redisService.setObject(cacheKey, result, 1800);

        return result;
    }

    @Override
    public PageResponse<UserResponse> findAllUsers(int page, int size)
            throws ChangeSetPersister.NotFoundException {

        String cacheKey = "users:" + page + ":" + size;

        PageResponse<UserResponse> cachedResult = redisService.getObject(cacheKey, PageResponse.class);
        if (cachedResult != null) {
            log.info("Cache hit for page: {} and size: {}", page, size);
            return cachedResult;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);

        if (users.isEmpty()) {
            throw new ChangeSetPersister.NotFoundException();
        }

        Page<UserResponse> result = users.map(user -> {
            UserResponse userResponse = userMapper.toUserResponse(user);
            if (user.getAddress() != null) {
                userResponse.setAddress(addressMapper.toAddressResponse(user.getAddress()));
            }
            return userResponse;
        });

        PageResponse<UserResponse> pageResponse = PageUtil.convertToUserPageResponse(result);

        redisService.setObject(cacheKey, result, 600);

        return pageResponse;
    }

    @Override
    public List<UserResponse> findAllUsers() throws ChangeSetPersister.NotFoundException {

        List<User> users = userRepository.findAll();
        if (users == null) {
            throw new ChangeSetPersister.NotFoundException();
        }

        if (!users.isEmpty()) {
            return users.stream().map(user -> {
                UserResponse response = userMapper.toUserResponse(user);
                response.setAddress(addressMapper.toAddressResponse(user.getAddress()));
                return response;
            }).toList();
        }
        return List.of();
    }



    /**
     * Web client
     private ApiResponse saveUserNode(String label) {
        UserNodeCreationRequest nodeRequest = new UserNodeCreationRequest();
        nodeRequest.setUsername(label);

        ApiResponse response = webClient.post()
                .uri("/recommendation-service/api/v1/friends/create")
                .bodyValue(nodeRequest)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .block();

        if (response == null || response.getCode() != 200) {
            log.error("Call server neo4j error: {}", response != null ? response.getMessage() : "No response");
            throw new AppException(EErrorCode.API_CALL_FAILED);
        }

        return response;
    }
     */
}
