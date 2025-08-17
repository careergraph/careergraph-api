package com.hcmute.careergraph.controller;


import com.hcmute.careergraph.dtos.request.auth.LoginRequest;
import com.hcmute.careergraph.dtos.request.auth.LogoutRequest;
import com.hcmute.careergraph.dtos.response.auth.LoginResponse;
import com.hcmute.careergraph.enums.ErrorType;
import com.hcmute.careergraph.exception.AppException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.services.IAuthService;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    IAuthService authService;

    @PostMapping("/login")
    RestResponse login(@RequestBody LoginRequest request)
            throws BadRequestException, ChangeSetPersister.NotFoundException {

        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BadRequestException("Request invalid");
        }

        LoginResponse result = authService.login(request);
        if (result == null) {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }

        return RestResponse.builder()
                .code(200)
                .message("Completed API: Login")
                .data(result)
                .build();
    }

    @PostMapping("/logout")
    RestResponse logout(@RequestBody LogoutRequest request) throws BadRequestException {

        if (request.getToken() == null) {
            throw new BadRequestException();
        }

        try {
            authService.logout(request);
        } catch (Exception e) {
            throw new AppException(ErrorType.UNAUTHORIZED);
        }

        return RestResponse.builder()
                .code(200)
                .message("Completed API: Logout")
                .build();
    }
}
