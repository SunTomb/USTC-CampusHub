package com.campushub.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_no", nullable = false, unique = true)
    private String studentNo;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "real_name", nullable = false)
    private String realName;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(nullable = false)
    private String status;

    protected User() {
    }

    public User(
            String studentNo,
            String username,
            String passwordHash,
            String realName,
            String nickname,
            String phone,
            String email,
            String status) {
        this.studentNo = studentNo;
        this.username = username;
        this.passwordHash = passwordHash;
        this.realName = realName;
        this.nickname = nickname;
        this.phone = phone;
        this.email = email;
        this.creditScore = 100;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRealName() {
        return realName;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public String getStatus() {
        return status;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }
}
