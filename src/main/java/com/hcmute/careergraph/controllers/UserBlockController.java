package com.hcmute.careergraph.controllers;

import com.hcmute.careergraph.exception.ForbiddenException;
import com.hcmute.careergraph.helper.RestResponse;
import com.hcmute.careergraph.helper.SecurityUtils;
import com.hcmute.careergraph.persistence.dtos.request.MessagingRequests;
import com.hcmute.careergraph.persistence.dtos.response.MessagingResponses;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.services.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserBlockController {

  private final MessageService messageService;
  private final SecurityUtils securityUtils;

  @PostMapping("/{candidateId}/block")
  public RestResponse<Void> blockUser(@PathVariable String candidateId,
      @Valid @RequestBody(required = false) MessagingRequests.BlockUserRequest request) {
    messageService.blockCandidate(getCurrentAccount(), candidateId, request != null ? request.getReason() : null);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Đã chặn ứng viên")
        .build();
  }

  @DeleteMapping("/{candidateId}/block")
  public RestResponse<Void> unblockUser(@PathVariable String candidateId) {
    messageService.unblockCandidate(getCurrentAccount(), candidateId);

    return RestResponse.<Void>builder()
        .status(HttpStatus.OK)
        .message("Đã bỏ chặn ứng viên")
        .build();
  }

  @GetMapping("/{candidateId}/block")
  public RestResponse<MessagingResponses.BlockStatusDto> getBlockStatus(@PathVariable String candidateId) {
    MessagingResponses.BlockStatusDto data = messageService.getBlockStatus(getCurrentAccount(), candidateId);

    return RestResponse.<MessagingResponses.BlockStatusDto>builder()
        .status(HttpStatus.OK)
        .message("Lấy trạng thái chặn thành công")
        .data(data)
        .build();
  }

  @GetMapping("/blocked")
  public RestResponse<List<MessagingResponses.BlockedUserDto>> getBlockedUsers() {
    List<MessagingResponses.BlockedUserDto> data = messageService.getBlockedCandidates(getCurrentAccount());

    return RestResponse.<List<MessagingResponses.BlockedUserDto>>builder()
        .status(HttpStatus.OK)
        .message("Lấy danh sách đã chặn thành công")
        .data(data)
        .build();
  }

  private Account getCurrentAccount() {
    return securityUtils.getCurrentAccount()
        .orElseThrow(() -> new ForbiddenException("Authentication required"));
  }
}
