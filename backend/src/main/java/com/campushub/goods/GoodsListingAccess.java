package com.campushub.goods;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** One live listing; detail and catalog images share the same audience. Never grants contact or order access. */
@Component
public class GoodsListingAccess {
    public static final String DELETED = "DELETED";
    public static final String DELETED_MESSAGE = "该商品已删除";
    private final JdbcTemplate db;

    public GoodsListingAccess(JdbcTemplate db) { this.db = db; }

    public static boolean isPublic(String status) { return "PUBLISHED".equals(status) || "ON_SALE".equals(status); }
    public static boolean canEdit(String status) { return isPublic(status) || "SOLD".equals(status); }

    public void requireReadable(long id, long sellerId, String status, Long viewerId) {
        if (DELETED.equals(status)) throw new ResponseStatusException(HttpStatus.GONE, DELETED_MESSAGE);
        if (isPublic(status) || (viewerId != null && (viewerId == sellerId || hasBuyingRecord(id, viewerId)))) return;
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在或暂不可查看");
    }

    private boolean hasBuyingRecord(long id, long viewerId) {
        return Boolean.TRUE.equals(db.queryForObject("""
            select exists(select 1 from goods_intents where goods_id=? and buyer_id=?)
                or exists(select 1 from goods_orders where goods_id=? and buyer_id=?)
            """, Boolean.class, id, viewerId, id, viewerId));
    }
}
