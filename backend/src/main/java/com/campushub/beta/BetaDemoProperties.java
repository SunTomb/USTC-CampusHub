package com.campushub.beta;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campushub.beta")
public class BetaDemoProperties {

    private boolean demoResetEnabled;
    private DemoAccount student = new DemoAccount();
    private DemoAccount admin = new DemoAccount();

    public boolean isDemoResetEnabled() {
        return demoResetEnabled;
    }

    public void setDemoResetEnabled(boolean demoResetEnabled) {
        this.demoResetEnabled = demoResetEnabled;
    }

    public DemoAccount getStudent() {
        return student;
    }

    public void setStudent(DemoAccount student) {
        this.student = student;
    }

    public DemoAccount getAdmin() {
        return admin;
    }

    public void setAdmin(DemoAccount admin) {
        this.admin = admin;
    }

    public static class DemoAccount {
        private String email;
        private String username;
        private String password;
        private String studentNo;
        private String realName;
        private String nickname;
        private String phone;
        private String wechatContact;
        private String qqContact;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getStudentNo() {
            return studentNo;
        }

        public void setStudentNo(String studentNo) {
            this.studentNo = studentNo;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getWechatContact() {
            return wechatContact;
        }

        public void setWechatContact(String wechatContact) {
            this.wechatContact = wechatContact;
        }

        public String getQqContact() {
            return qqContact;
        }

        public void setQqContact(String qqContact) {
            this.qqContact = qqContact;
        }
    }
}
