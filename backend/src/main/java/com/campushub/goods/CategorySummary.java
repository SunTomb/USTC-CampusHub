package com.campushub.goods;

public record CategorySummary(
        Long id,
        Long parentId,
        String name,
        Integer sortOrder,
        Boolean enabled) {

    public static CategorySummary from(Category category) {
        return new CategorySummary(
                category.getId(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getName(),
                category.getSortOrder(),
                category.getEnabled());
    }
}
