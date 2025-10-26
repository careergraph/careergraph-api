package com.hcmute.careergraph.helper;

import com.hcmute.careergraph.enums.common.Role;
import com.hcmute.careergraph.persistence.models.Account;
import com.hcmute.careergraph.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class SecurityUtils {

   private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);
   private final AccountRepository accountRepository;

   private Authentication getAuthentication() {
      return SecurityContextHolder.getContext().getAuthentication();
   }

   public Optional<Account> getCurrentAccount() {
      Authentication auth = getAuthentication();
      if (auth == null) {
         LOG.debug("No authentication found");
         return Optional.empty();
      }

      Object principal = auth.getPrincipal();

      try {
         if (principal instanceof Account account) {
            return Optional.of(account);
         } else if (principal instanceof UserDetails userDetails) {
            return accountRepository.findByEmail(userDetails.getUsername());
         } else if (principal instanceof String username) {
            return accountRepository.findByEmail(username);
         } else if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            return accountRepository.findByEmail(email);
         }
      } catch (Exception e) {
         LOG.error(e.getMessage());
      }

      return Optional.empty();
   }

   public Optional<String> getCurrentEmail() {
      return getCurrentAccount().map(Account::getEmail);
   }

   public Optional<Role> getCurrentRole() {
      return getCurrentAccount().map(Account::getRole);
   }

   public boolean isAdmin() {
      return getCurrentRole().map(role -> role == Role.ADMIN).orElse(false);
   }

   public Optional<String> getCandidateId() {
      return getCurrentAccount()
              .map(Account::getCandidate)
              .map(c -> c.getId());
   }

   public Optional<String> getCompanyId() {
      return getCurrentAccount()
              .map(Account::getCandidate)
              .map(c -> c.getId());
   }
}
