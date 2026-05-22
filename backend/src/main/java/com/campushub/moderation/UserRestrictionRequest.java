package com.campushub.moderation;

public record UserRestrictionRequest(String restrictionType, String reason, Integer days) {
}
