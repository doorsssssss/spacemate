package com.spacemate.modules.auth.service;

import com.spacemate.domain.entity.AppUser;
import java.util.Optional;

/**
 * 用户服务接口。
 */
public interface UserService {
    Optional<AppUser> findByPhone(String phone);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findById(long id);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    AppUser createUser(AppUser user);

    void updatePassword(AppUser user);
}
