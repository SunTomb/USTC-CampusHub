package com.campushub.user;

import jakarta.validation.constraints.Size;

public record UpdateContactsRequest(
        @Size(max = 120, message = "微信号不能超过 120 个字符")
        String wechatContact,
        @Size(max = 60, message = "QQ 号不能超过 60 个字符")
        String qqContact) {
}
