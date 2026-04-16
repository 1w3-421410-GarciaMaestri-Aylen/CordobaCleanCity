package com.example.garbagereporting.service;

import com.example.garbagereporting.model.UserAccount;

public interface EmailVerificationService {

    void issueVerificationToken(UserAccount user);
}
