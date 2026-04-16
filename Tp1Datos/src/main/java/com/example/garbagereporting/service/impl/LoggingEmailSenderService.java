package com.example.garbagereporting.service.impl;

import com.example.garbagereporting.config.ApplicationProperties;
import com.example.garbagereporting.model.UserAccount;
import com.example.garbagereporting.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingEmailSenderService implements EmailSenderService {

    private final ApplicationProperties properties;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Override
    public void sendVerificationEmail(UserAccount user, String verificationLink) {
        if (Boolean.TRUE.equals(properties.getMail().getMockEnabled())) {
            log.info(
                    "MOCK verification email -> to={} from={} link={}",
                    user.getEmail(),
                    properties.getMail().getFrom(),
                    verificationLink
            );
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("MAIL_MOCK_ENABLED=false but JavaMailSender is not configured");
        }

        String recipientName = buildRecipientName(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getMail().getFrom());
        message.setTo(user.getEmail());
        message.setSubject("Verifica tu cuenta en EcoRuta");
        message.setText("""
                Hola %s,

                Gracias por registrarte en EcoRuta.
                Para activar tu cuenta, verifica tu email en este enlace:

                %s

                Si no solicitaste este registro, puedes ignorar este mensaje.
                """.formatted(recipientName, verificationLink));

        try {
            mailSender.send(message);
            log.info("Verification email sent -> to={} from={}", user.getEmail(), properties.getMail().getFrom());
        } catch (MailException ex) {
            log.error("Failed to send verification email -> to={}", user.getEmail(), ex);
            throw new IllegalStateException("No se pudo enviar el email de verificacion", ex);
        }
    }

    private String buildRecipientName(UserAccount user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "usuario" : fullName;
    }
}
