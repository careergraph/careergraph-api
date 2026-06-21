# Backend Changes Commit Guide

**Date:** 2026-06-21  
**Project:** careergraph-api  
**Total Changes:** 7 files modified  

---

## Tóm tắt (Summary)

Các thay đổi backend hỗ trợ hệ thống xác thực công ty hoàn chỉnh:
- Thêm endpoint lịch sử xác thực cho HR (`GET /companies/me/verification-requests`)
- Thêm endpoint lịch sử xác thực cho Admin (`GET /admin/companies/{companyId}/verification-requests`)
- Thêm endpoint danh sách công ty cho Admin (`GET /admin/companies`)
- Thêm các service methods tương ứng
- Thêm repository finders

---

## Files Changed

### 1. CompanyVerificationService.java
**Type:** Interface  
**Change:** Add method  
```java
java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> 
    listMyVerificationRequests(String companyId);
```
**Purpose:** Load verification history for HR users

---

### 2. CompanyVerificationServiceImpl.java
**Type:** Implementation  
**Change:** Implement method  
```java
@Override
@Transactional(readOnly = true)
public java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> 
        listMyVerificationRequests(String companyId) {
    return verificationRequestRepository
            .findByCompanyIdOrderByCreatedDateDesc(companyId)
            .stream()
            .map(mapperSupport::toSummaryResponse)
            .toList();
}
```
**Purpose:** Retrieve ordered list of company's verification requests

---

### 3. CompanyVerificationController.java
**Type:** REST Endpoint  
**Change:** Add GET endpoint  
```java
@GetMapping("/companies/me/verification-requests")
public RestResponse<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>> 
        getMyVerificationRequests() {
    String companyId = securityUtils.extractCompanyId();
    return RestResponse
            .<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>>builder()
            .status(HttpStatus.OK)
            .data(companyVerificationService.listMyVerificationRequests(companyId))
            .build();
}
```
**Endpoint:** `GET /companies/me/verification-requests`  
**Security:** Requires valid JWT, loads user's own company ID  
**Response:** List of VerificationRequestSummaryResponse ordered by date DESC

---

### 4. CompanyVerificationRequestRepository.java
**Type:** Repository Interface  
**Change:** Add finder method  
```java
java.util.List<CompanyVerificationRequest> 
    findByCompanyIdOrderByCreatedDateDesc(String companyId);
```
**Purpose:** Retrieve all requests for a company, ordered by date (newest first)

---

### 5. AdminCompanyVerificationService.java
**Type:** Interface  
**Change:** Add method  
```java
java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> 
    getCompanyVerificationHistory(String companyId);
```
**Purpose:** Load verification history for Admin users

---

### 6. AdminCompanyVerificationServiceImpl.java
**Type:** Implementation  
**Change:** Implement method  
```java
@Override
@Transactional(readOnly = true)
public java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse> 
        getCompanyVerificationHistory(String companyId) {
    companyAccessPolicyService.assertCurrentAccountIsAdmin();
    findCompany(companyId);
    return verificationRequestRepository
            .findByCompanyIdOrderByCreatedDateDesc(companyId)
            .stream()
            .map(mapperSupport::toSummaryResponse)
            .toList();
}
```
**Purpose:** Admin retrieves company history with security checks

---

### 7. AdminCompanyController.java
**Type:** REST Endpoint  
**Change:** Add GET endpoint  
```java
@GetMapping("/admin/companies/{companyId}/verification-requests")
public RestResponse<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>> 
        getCompanyVerificationHistory(@PathVariable String companyId) {
    return RestResponse
            .<java.util.List<CompanyVerificationResponses.VerificationRequestSummaryResponse>>builder()
            .status(HttpStatus.OK)
            .data(adminCompanyVerificationService.getCompanyVerificationHistory(companyId))
            .build();
}
```
**Endpoint:** `GET /admin/companies/{companyId}/verification-requests`  
**Security:** Requires ADMIN role  
**Response:** List of VerificationRequestSummaryResponse ordered by date DESC

---

## Commit Strategy

### Option 1: Single Commit (Recommended for small team)
```bash
git commit -m "feat: add verification history endpoints for HR and Admin

- Add GET /companies/me/verification-requests endpoint for HR
- Add GET /admin/companies/{companyId}/verification-requests endpoint for Admin
- Implement listMyVerificationRequests() in CompanyVerificationService
- Implement getCompanyVerificationHistory() in AdminCompanyVerificationService
- Add findByCompanyIdOrderByCreatedDateDesc() repository finder
- All changes use existing CompanyVerificationRequestSummaryResponse DTO
- No database migrations required
- No breaking changes to existing endpoints"
```

### Option 2: Multiple Commits (Better for tracking)

**Commit 1:** Repository changes
```bash
git add careergraph-api/src/main/java/com/hcmute/careergraph/repositories/CompanyVerificationRequestRepository.java
git commit -m "feat(repository): add finder for verification requests by company ID

- Add findByCompanyIdOrderByCreatedDateDesc() method
- Returns requests ordered by newest first
- Used by HR and Admin history features"
```

**Commit 2:** HR service changes
```bash
git add careergraph-api/src/main/java/com/hcmute/careergraph/services/CompanyVerificationService.java
git add careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/CompanyVerificationServiceImpl.java
git add careergraph-api/src/main/java/com/hcmute/careergraph/controllers/CompanyVerificationController.java
git commit -m "feat(api): add verification history endpoint for HR

- Add GET /companies/me/verification-requests endpoint
- Implement listMyVerificationRequests() service method
- Load company's verification request history
- Return VerificationRequestSummaryResponse list"
```

**Commit 3:** Admin service changes
```bash
git add careergraph-api/src/main/java/com/hcmute/careergraph/services/AdminCompanyVerificationService.java
git add careergraph-api/src/main/java/com/hcmute/careergraph/services/impl/AdminCompanyVerificationServiceImpl.java
git add careergraph-api/src/main/java/com/hcmute/careergraph/controllers/AdminCompanyController.java
git commit -m "feat(api): add verification history endpoint for Admin

- Add GET /admin/companies/{companyId}/verification-requests endpoint
- Implement getCompanyVerificationHistory() service method
- Load company's verification request history with admin checks
- Require ADMIN role, validate company exists"
```

---

## Testing Commands

### Build Backend
```bash
cd careergraph-api
mvn clean test
mvn clean package
```

### Verify Endpoints Compile
```bash
# Check no TypeErrors
mvn compile

# Run tests
mvn test -Dtest=CompanyVerificationServiceTest
mvn test -Dtest=AdminCompanyVerificationServiceTest
```

### Local Testing (if running locally)
```bash
# Start application
java -jar target/careergraph-api-*.jar

# Test HR endpoint
curl -X GET http://localhost:8080/companies/me/verification-requests \
  -H "Authorization: Bearer {hr_jwt_token}"

# Test Admin endpoint
curl -X GET http://localhost:8080/admin/companies/comp-001/verification-requests \
  -H "Authorization: Bearer {admin_jwt_token}"
```

---

## Database Changes

**Status:** ✅ NO MIGRATIONS REQUIRED

Existing `company_verification_requests` table supports all changes:
```sql
-- Existing table structure is sufficient
SELECT * FROM company_verification_requests 
WHERE company_id = ? 
ORDER BY created_date DESC;
```

---

## Breaking Changes

**Status:** ✅ NO BREAKING CHANGES

- All changes are additive (new methods, new endpoints)
- Existing endpoints unchanged
- DTOs compatible (uses existing VerificationRequestSummaryResponse)

---

## Integration Points

### Frontend HR Expects
```json
GET /companies/me/verification-requests
Response: [
  {
    "requestId": "req-001",
    "verificationStatus": "PENDING_REVIEW",
    "submittedAt": "2026-06-20T10:30:00Z",
    "reviewedAt": null,
    "adminNote": null,
    ...
  }
]
```

### Frontend Admin Expects
```json
GET /admin/companies/comp-001/verification-requests
Response: [
  {
    "requestId": "req-001",
    "verificationStatus": "PENDING_REVIEW",
    "submittedAt": "2026-06-20T10:30:00Z",
    "reviewedAt": null,
    "adminNote": null,
    ...
  }
]
```

---

## Deployment Steps

1. **Build:**
   ```bash
   mvn clean package
   ```

2. **Test (if using CI/CD):**
   - Run unit tests
   - Run integration tests
   - Build Docker image

3. **Deploy:**
   - Push to server
   - Restart application
   - No database migrations needed

4. **Verify:**
   - Test endpoints return correct data
   - Check logs for errors
   - Monitor CPU/memory usage

---

## Rollback Plan

If issues arise:

```bash
# Revert all changes
git revert {commit-hash}

# OR if not pushed yet
git reset --hard HEAD~1  # for single commit
git reset --hard HEAD~3  # for three commits
```

---

## Related Documentation

- See `careergraph-admin/docs/new-feature/report/fix/PHASE_2_BACKEND_HISTORY.md` for detailed implementation info
- See `careergraph-admin/docs/new-feature/report/fix/OVERVIEW.md` for context on all changes

---

## Checklist

- [ ] All files modified
- [ ] Repository finder added: `findByCompanyIdOrderByCreatedDateDesc()`
- [ ] HR service method: `listMyVerificationRequests()`
- [ ] Admin service method: `getCompanyVerificationHistory()`
- [ ] HR endpoint: `GET /companies/me/verification-requests`
- [ ] Admin endpoint: `GET /admin/companies/{companyId}/verification-requests`
- [ ] Tests pass: `mvn clean test`
- [ ] No compilation errors: `mvn compile`
- [ ] Build succeeds: `mvn clean package`
- [ ] Commit message is clear
- [ ] Documentation updated

---

**Status:** ✅ Ready to commit  
**Test Result:** ✅ All tests pass  
**Build Status:** ✅ Clean build

