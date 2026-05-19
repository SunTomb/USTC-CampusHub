package com.campushub.wallet;

import com.campushub.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "wallet_accounts")
public class WalletAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "frozen_balance", nullable = false)
    private BigDecimal frozenBalance;

    @Column(nullable = false)
    private String status;

    protected WalletAccount() {
    }

    public WalletAccount(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public String getStatus() {
        return status;
    }
}
