package com.campushub.orders;

/** A projection, not a new order table. Every branch restricts rows to the JWT actor. */
final class OrderDeskQuery {
    private OrderDeskQuery() {}
    static final String BASE = """
        select 'TASK' kind,t.id rec_id,t.id object_id,t.title,
          case when t.publisher_id=:uid then 'PUBLISHER' when a.id=t.accepted_application_id then 'RUNNER' else 'APPLICANT' end actor_role,
          case when t.publisher_id=:uid or a.id=t.accepted_application_id then
            case when t.workflow_status='PUBLISHED' and t.deadline<current_timestamp then 'EXPIRED' else t.workflow_status end
            when t.workflow_status='CANCELLED' then 'CANCELLED' when a.status in ('PENDING','APPLIED') and t.deadline<current_timestamp then 'EXPIRED' else a.status end raw_status,
          case when t.publisher_id=:uid then chosen.applicant_id when a.id=t.accepted_application_id then t.publisher_id end peer,
          t.reward_amount amount,case when t.publisher_id=:uid then t.created_at else a.created_at end created_at,
          case when t.publisher_id=:uid then (select count(*) from task_applications z where z.task_id=t.id and z.status in ('APPLIED','PENDING')) else 0 end interested
        from reward_tasks t left join task_applications a on a.task_id=t.id and a.applicant_id=:uid
        left join task_applications chosen on chosen.id=t.accepted_application_id where t.publisher_id=:uid or a.id is not null
        union all
        select 'GOODS_LISTING',g.id,g.id,g.title,'SELLER',g.status,null,g.price,g.created_at,
          (select count(*) from goods_intents i where i.goods_id=g.id and i.status='OPEN')
        from goods g where g.seller_id=:uid and g.status<>'DELETED' and not exists(select 1 from goods_intents i where i.goods_id=g.id and i.status='COMPLETED' and i.buyer_id=g.sold_to_user_id)
        and not exists(select 1 from goods_orders o where o.goods_id=g.id and o.status='COMPLETED')
        union all
        select 'GOODS_INTENT',i.id,g.id,g.title,case when i.buyer_id=:uid then 'BUYER' else 'SELLER' end,
          case when i.status='OPEN' and g.status<>'PUBLISHED' then 'CLOSED' else i.status end,
          case when i.status='COMPLETED' and i.buyer_id=g.sold_to_user_id then (case when i.buyer_id=:uid then i.seller_id else i.buyer_id end) end,
          case when i.status='COMPLETED' then i.settled_amount when g.status='DELETED' then null else g.price end,
          i.created_at,0 from goods_intents i join goods g on g.id=i.goods_id
        where i.buyer_id=:uid or (i.seller_id=:uid and i.status='COMPLETED' and i.buyer_id=g.sold_to_user_id)
        union all
        select 'GOODS_ORDER',o.id,g.id,g.title,case when o.buyer_id=:uid then 'BUYER' else 'SELLER' end,o.status,
          case when o.buyer_id=:uid then o.seller_id else o.buyer_id end,o.amount,o.created_at,0
        from goods_orders o join goods g on g.id=o.goods_id where o.buyer_id=:uid or o.seller_id=:uid
        union all
        select 'BOOKING',o.id,i.id,i.title,case when o.customer_id=:uid then 'CUSTOMER' else 'MERCHANT' end,o.status,
          case when o.customer_id=:uid then o.provider_id else o.customer_id end,o.amount,o.created_at,0
        from service_orders o join service_items i on i.id=o.service_item_id where o.customer_id=:uid or o.provider_id=:uid
        union all
        select 'PROJECT',p.id,p.id,p.title,'AUTHOR',case when p.status='APPROVED' and p.expires_at<current_timestamp then 'EXPIRED' else p.status end,
          null,null,p.created_at,0 from project_ads p where p.publisher_id=:uid
        """;
    static final String ANNOTATED="select b.*, case when r.id is null then 0 else 1 end reviewed, case when c.id is null then 0 else 1 end complained from ("+BASE+") b left join reviews r on r.reviewer_id=:uid and r.target_type=concat('DESK_',b.kind) and r.target_id=b.rec_id left join report_records c on c.order_dedupe_key=concat(:uid,':',b.kind,':',b.rec_id)";
    static final String RECORDS="""
        select a.*, case
          when raw_status='COMPLETED' and peer is not null and peer<>:uid and reviewed=0 then 'REVIEW'
          when raw_status in ('COMPLETED','CANCELLED','CANCELED','REJECTED','OFF_SHELF','SOLD','CLOSED','EXPIRED','BLOCKED','FAILED') then 'ENDED'
          when (kind='GOODS_LISTING' and interested=0 and raw_status='PUBLISHED') or (kind='PROJECT' and raw_status='APPROVED') then 'ACTIVE'
          when raw_status in ('PUBLISHED','OPEN','PENDING','APPLIED','REQUESTED','PENDING_REVIEW','DRAFT','CREATED','PENDING_CONFIRMATION') then 'PENDING'
          else 'ACTIVE' end stage from (
        """+ANNOTATED+") a";
}
