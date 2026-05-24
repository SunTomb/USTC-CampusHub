package com.campushub.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campushub.common.BusinessException;
import java.util.Iterator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class RegisterMailServiceTest {

    @Test
    void enabledMailWithoutSenderFailsClearly() {
        MailProperties properties = new MailProperties(
                true,
                "smtp",
                new MailProperties.Smtp("smtp.example.com", 587, "user", "secret", "noreply@example.com", "CampusHub"),
                new MailProperties.Code(10, 60));
        RegisterMailService service = new RegisterMailService(new EmptyMailSenderProvider(), properties);

        assertThatThrownBy(() -> service.sendRegisterCode("student@mail.ustc.edu.cn", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮件服务未配置");
    }

    private static class EmptyMailSenderProvider implements ObjectProvider<JavaMailSender> {
        @Override
        public JavaMailSender getObject(Object... args) throws BeansException {
            return null;
        }

        @Override
        public JavaMailSender getIfAvailable() throws BeansException {
            return null;
        }

        @Override
        public JavaMailSender getIfUnique() throws BeansException {
            return null;
        }

        @Override
        public JavaMailSender getObject() throws BeansException {
            return null;
        }

        @Override
        public Iterator<JavaMailSender> iterator() {
            return Stream.<JavaMailSender>empty().iterator();
        }
    }
}
