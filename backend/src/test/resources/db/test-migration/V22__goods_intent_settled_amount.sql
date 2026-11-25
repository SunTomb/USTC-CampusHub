-- One amount per verified offline sale, not a copy or a revision of the listing.
ALTER TABLE goods_intents ADD COLUMN settled_amount DECIMAL(10,2) NULL;
-- Previously the app did not allow editing sold goods. Preserve the existing displayed
-- amount only for a known winner; do not invent a sale for an unmatched historical intent.
UPDATE goods_intents SET settled_amount=(SELECT g.price FROM goods g
 WHERE g.id=goods_intents.goods_id AND g.sold_to_user_id=goods_intents.buyer_id AND g.status='SOLD')
 WHERE status='COMPLETED';
