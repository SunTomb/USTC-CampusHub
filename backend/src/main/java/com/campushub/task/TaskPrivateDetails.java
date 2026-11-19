package com.campushub.task;

public record TaskPrivateDetails(String pickupCode, String privatePickupNotes) {

    public static TaskPrivateDetails from(RewardTask task) {
        return new TaskPrivateDetails(task.getPickupCode(), task.getPrivatePickupNotes());
    }
}
