package com.uniwise.identity_service.modules.account.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.AccountUpdateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AccountError;
import com.uniwise.common.exception.errors.RoleError;
import com.uniwise.grpc_spring_boot_starter.annotation.GrpcClient;
import com.uniwise.identity_service.modules.account.AccountService;
import com.uniwise.identity_service.modules.account.entity.Account;
import com.uniwise.identity_service.modules.account.mapper.AccountMapper;
import com.uniwise.identity_service.modules.account.repository.AccountRepository;
import com.uniwise.identity_service.modules.role.RoleService;
import com.uniwise.identity_service.modules.role.entity.Role;
import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.ProfileServiceGrpc.ProfileServiceBlockingStub;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountServiceImpl implements AccountService {
    @NonFinal
    @GrpcClient("user-service")
    ProfileServiceBlockingStub profileServiceClient;

    AccountRepository accountRepository;
    AccountMapper accountMapper;
    PasswordEncoder passwordEncoder;
    RoleService roleService;
    String provider = "UNIWISE";
    Set<String> defaultRoles = Set.of("USER");

    @Override
    @Transactional(rollbackOn = Exception.class)
    public AccountResponse create(AccountCreateRequest request) {
        if (accountRepository.existsByEmailAndProvider(request.getEmail(), provider))
            throw new HttpException(AccountError.ACCOUNT_ALREADY_EXISTS);

        Account account = accountMapper.toEntity(request);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        Set<Role> roles = roleService.getByNames(defaultRoles);
        if (roles.isEmpty())
            throw new HttpException(AccountError.DEFAULT_ROLES_NOT_FOUND);
        account.setRoles(roles);
        Account saved = accountRepository.save(account);

        CreateProfileRequest profileRequest = CreateProfileRequest.newBuilder()
                .setAccountId(saved.getId())
                .setEmail(saved.getEmail())
                .setName(request.getName())
                .build();

        CreateProfileResponse profileResponse = profileServiceClient.createProfile(profileRequest);
        log.info("Profile created successfully: {}", profileResponse.getProfile().toString());
        log.info("Account created successfully with id: {}", saved.getId());
        return accountMapper.toResponse(saved);
    }

    @Override
    public AccountResponse getById(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
        return accountMapper.toResponse(account);
    }

    // TODO: Method này sẽ được thay thế bằng elasticsearch hoặc search engine khác
    // trong tương lai để có hiệu năng tốt hơn
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PageResponse<AccountResponse> getAll(int page, int size, String keyword,
            Boolean isActive, String sortBy, String sortDir) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, orderBy));

        Page<Account> accounts = accountRepository.searchAccounts(normalizedKeyword, isActive, pageable);
        List<AccountResponse> content = accounts.getContent().stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<AccountResponse>builder()
                .content(content)
                .pageNumber(accounts.getNumber())
                .pageSize(accounts.getSize())
                .totalElements(accounts.getTotalElements())
                .totalPages(accounts.getTotalPages())
                .last(accounts.isLast())
                .build();
    }

    @Override
    @Transactional
    public AccountResponse getProfile() {
        SecurityContext context = SecurityContextHolder.getContext();
        log.info(":::: contex: {}", context);
        String accountId = context.getAuthentication().getName();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse update(String id, AccountUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));

        if (request.getEmail() != null && accountRepository.existsByEmailAndIdNot(request.getEmail(), id))
            throw new HttpException(AccountError.ACCOUNT_ALREADY_EXISTS);

        if (request.getEmail() != null)
            account.setEmail(request.getEmail());

        if (request.getPassword() != null)
            account.setPassword(passwordEncoder.encode(request.getPassword()));

        Account updated = accountRepository.save(account);
        log.info("Account updated successfully with id: {}", updated.getId());
        return accountMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
        accountRepository.delete(account);
        log.info("Account deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
        account.setIsActive(!Boolean.TRUE.equals(account.getIsActive()));
        accountRepository.save(account);
        log.info("Account active status toggled for id: {}", id);
    }

    @Override
    public Account getEntityById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
    }

    @Override
    public Account getByEmail(String email) {
        return accountRepository.findByEmailAndProvider(email, provider)
                .orElseThrow(() -> new HttpException(AccountError.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse assignRoles(String id, Set<String> roleNames) {
        Account account = getEntityById(id);
        Set<Role> rolesToAdd = roleService.getByNames(roleNames);
        // TODO: roles not found error info
        if (rolesToAdd.isEmpty())
            throw new HttpException(RoleError.ROLE_NOT_FOUND);
        account.getRoles().addAll(rolesToAdd);
        Account updated = accountRepository.save(account);
        log.info("Roles {} assigned to account with id: {}", roleNames, id);
        return accountMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse revokeRoles(String id, Set<String> roleNames) {
        Account account = getEntityById(id);
        Set<Role> rolesToRemove = roleService.getByNames(roleNames);
        // TODO: roles not found error info
        if (rolesToRemove.isEmpty())
            throw new HttpException(RoleError.ROLE_NOT_FOUND);
        account.getRoles().removeAll(rolesToRemove);
        Account updated = accountRepository.save(account);
        log.info("Roles {} revoked from account with id: {}", roleNames, id);
        return accountMapper.toResponse(updated);
    }
}
