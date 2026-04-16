package com.example.garbagereporting.service;

import com.example.garbagereporting.model.UserAccount;

public interface EmailSenderService {

    void sendVerificationEmail(UserAccount user, String verificationLink);
}
