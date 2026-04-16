# Phase 1 API Summary - Messaging and Notifications

This document lists the backend APIs implemented in phase 1 for FE integration.

## Base Context Path

- /careergraph/api/v1/messages
- /careergraph/api/v1/notifications

## Messaging APIs

### GET /messages/threads?page=0&size=20

Purpose:
- Get inbox threads of current user (HR or Candidate).

Response data:
- RestResponse<Page<ThreadSummaryDto>>

### POST /messages/threads

Purpose:
- Create or get existing thread (idempotent).

Request body:
{
  "candidateId": "optional-for-HR",
  "companyId": "optional-for-candidate",
  "applicationId": "optional"
}

Response data:
- RestResponse<ThreadSummaryDto>

### GET /messages/threads/{threadId}

Purpose:
- Get thread detail if current user has access.

Response data:
- RestResponse<ThreadSummaryDto>

### GET /messages/threads/{threadId}/messages?page=0&size=30

Purpose:
- Get paginated message history (ascending by created time).

Response data:
- RestResponse<Page<MessageDto>>

### POST /messages/threads/{threadId}/messages

Purpose:
- Send a new message.

Request body:
{
  "content": "Hello",
  "contentType": "TEXT",
  "fileUrl": null,
  "fileName": null,
  "fileSize": null
}

Response data:
- RestResponse<MessageDto>

### POST /messages/threads/{threadId}/read

Purpose:
- Mark thread as read for current user.

Response data:
- RestResponse<Void>

### DELETE /messages/{messageId}

Purpose:
- Soft-delete a sent message (sender only).

Response data:
- RestResponse<Void>

### GET /messages/unread-count

Purpose:
- Get total unread message count for badge.

Response data:
{
  "status": "OK",
  "message": "Unread count retrieved successfully",
  "data": {
    "count": 3
  }
}

## Notification APIs

### GET /notifications?page=0&size=20

Purpose:
- Get paginated notifications of current user.

Response data:
- RestResponse<NotificationPageDto>

### POST /notifications/{id}/read

Purpose:
- Mark one notification as read.

Response data:
- RestResponse<Void>

### POST /notifications/read-all

Purpose:
- Mark all notifications as read.

Response data:
- RestResponse<Void>

### GET /notifications/unread-count

Purpose:
- Get unread notification count for badge.

Response data:
{
  "status": "OK",
  "message": "Unread count retrieved successfully",
  "data": {
    "count": 5
  }
}

## Notification Hooks Implemented

- Application created -> NEW_APPLICATION notification to company account.
- Application stage changed -> APPLICATION_STATUS_CHANGED notification to candidate account.
- New message sent -> NEW_MESSAGE notification to the opposite participant account.
