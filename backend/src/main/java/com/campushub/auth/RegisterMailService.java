package com.campushub.auth;

import com.campushub.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RegisterMailService {

    private static final Logger log = LoggerFactory.getLogger(RegisterMailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public RegisterMailService(ObjectProvider<JavaMailSender> mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender.getIfAvailable();
        this.mailProperties = mailProperties;
    }

    public void sendRegisterCode(String email, String code) {
        if (!mailProperties.enabled()) {
            log.info("Registration email verification mock-sent to {}", maskEmail(email));
            return;
        }
        if (mailSender == null) {
            throw new BusinessException("邮件服务未配置，请联系管理员开启 SMTP");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(formatFrom());
        message.setTo(email);
        message.setSubject("校集 CampusHub 注册验证码");
        message.setText("你正在注册校集 CampusHub。验证码有效期 "
                + mailProperties.code().ttlMinutes()
                + " 分钟。若非本人操作，请忽略本邮件。\n\n验证码："
                + code);
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            throw new BusinessException("验证码邮件发送失败，请稍后再试");
        }
        log.info("Registration email verification sent to {} via {}", maskEmail(email), mailProperties.provider());
    }

    private String formatFrom() {
        String from = mailProperties.smtp().from();
        String fromName = mailProperties.smtp().fromName();
        if (fromName == null || fromName.isBlank()) {
            return from;
        }
        return fromName + " <" + from + ">";
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
