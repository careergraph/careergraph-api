# Account Management & Profile Sync (Production Ready)

## Overview
Implement a unified account management system for both **Candidate (Ứng viên)** and **HR**, including:
- Email update with OTP verification
- Password change with OTP verification
- HR profile management (update personal information)
- Candidate profile view page integration with existing logic
- Ensure data consistency between HR update view and Candidate view

---

## 1. Feature Scope

### 1.1 Common (Candidate + HR)

#### 🔐 Email Update (OTP Required)
- User enters new email
- System sends OTP to new email
- User verifies OTP
- Only after OTP success → update email

#### 🔑 Password Change (OTP Required)
- User enters:
  - Current password
  - New password
- System triggers OTP verification step
- User verifies OTP
- Only after OTP success → update password

---

### 1.2 HR – Profile Management (Update View)

HR has an existing UI for personal information management, but:
- Some logic is already implemented correctly → **DO NOT rewrite unnecessarily**
- Some features are still inactive → must be integrated properly

#### Required:
- Enable full update of HR profile information:
  - Name
  - Phone number
  - Address (reuse existing APIs if available)
  - Avatar
  - Cover image (if missing → consider adding if system supports or extend logically)

⚠️ Important:
- Reuse existing endpoints (do not duplicate API logic)
- If avatar/cover image is partially supported, extend carefully instead of rebuilding

---

### 1.3 Candidate – Profile View Page

- Candidate has a **profile detail page (view-only)**
- Existing logic already contains partial correct implementation → must be reused and integrated properly
- Ensure:
  - Display correct user information
  - Sync with HR-updated data
  - Follow existing logic patterns instead of rewriting from scratch

#### UI/UX consistency requirements:
- Ensure **Candidate view matches HR stored data**
- Reuse existing logic for fetching profile data

---

## 2. Data Consistency Requirements

### Critical Sync Rules:
- HR updates must reflect immediately in Candidate profile view
- Ensure avatar consistency across:
  - HR profile management page
  - Candidate profile view page

### Missing Feature Handling:
- HR profile currently does NOT have cover image support while Candidate view expects it
- AI should decide:
  - Whether to extend backend support
  - Or gracefully handle missing data in frontend (fallback UI)

---

## 3. Technical Requirements

### Backend
- Reuse existing endpoints whenever possible
- Extend only when necessary (e.g., missing cover image field)
- Ensure OTP flow consistency across email & password updates

### Frontend
- Integrate existing correct logic (do not rewrite working parts)
- Activate currently inactive UI components
- Ensure proper state handling:
  - loading
  - error
  - success

### OTP Flow Standard
1. Trigger update request
2. Send OTP
3. User input OTP
4. Verify OTP
5. If valid → apply update
6. If invalid → reject

---

## 4. AI Decision Authority (IMPORTANT)

The AI is allowed and expected to:
- Decide whether to extend missing features (e.g., cover image support)
- Decide whether to reuse or refactor existing logic
- Optimize architecture for production readiness
- Ensure consistency between HR and Candidate views
- Avoid unnecessary duplication or over-engineering

Goal:  
👉 Deliver a **clean, production-ready, maintainable implementation**, not just feature completion.

---

## 5. Constraints
- Do not duplicate existing APIs
- Do not rewrite working logic unnecessarily
- Maintain backward compatibility
- Ensure role-based separation (HR vs Candidate)
- Prioritize reuse over rebuild

---

## 6. Expected Outcome
- Email update works with OTP (Candidate + HR)
- Password change works with OTP (Candidate + HR)
- HR can fully update profile information
- Candidate profile page correctly displays synchronized data
- Avatar consistency across all views
- Cover image handled gracefully or extended properly
- System is production-ready and maintainable

---
