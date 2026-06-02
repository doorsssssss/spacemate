package com.spacemate.modules.auth.service;

import com.spacemate.domain.entity.SendCodeResult;
import com.spacemate.domain.entity.VerificationCheckResult;
import com.spacemate.domain.enums.VerificationScene;

public interface VerificationService {
    SendCodeResult sendCode(VerificationScene scene, String identifier);

    void invalidate(VerificationScene scene, String identifier);

    VerificationCheckResult verify(VerificationScene scene, String identifier, String code);



}

