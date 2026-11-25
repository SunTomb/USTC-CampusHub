package com.campushub.orders;

import com.campushub.common.BusinessException;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class CatalogRatings {
    public record Grade(BigDecimal score,long count){}
    public record Entry(String nickname,int rating,String content,LocalDateTime createdAt){}
    public record Feedback(BigDecimal score,long count,List<Entry> entries){}
    private final JdbcTemplate db;
    public CatalogRatings(JdbcTemplate db){this.db=db;}
    private static BigDecimal display(BigDecimal n){return n==null?null:n.setScale(1,RoundingMode.HALF_UP);}
    /** Evaluate each rated item once, before rounding either level. */
    public Map<Long,BigDecimal> shops(){
        Map<Long,BigDecimal> m=new HashMap<>();db.query("select x.shop_id,avg(x.mean_rating) from (select i.shop_id,i.id,avg(r.rating) mean_rating from service_items i join reviews r on r.rated_item_id=i.id group by i.shop_id,i.id) x group by x.shop_id",r->{m.put(r.getLong(1),display(r.getBigDecimal(2)));});return m;
    }
    public Map<Long,Grade> items(){Map<Long,Grade> m=new HashMap<>();db.query("select rated_item_id,avg(rating),count(*) from reviews where rated_item_id is not null group by rated_item_id",r->{m.put(r.getLong(1),new Grade(display(r.getBigDecimal(2)),r.getLong(3)));});return m;}
    public Grade forItem(long id){return db.queryForObject("select avg(rating),count(*) from reviews where rated_item_id=?",(r,n)->new Grade(display(r.getBigDecimal(1)),r.getLong(2)),id);}
    public Feedback read(long item,int page){
        if(page<0||page>100000||db.queryForObject("select count(*) from service_items i join shops s on s.id=i.shop_id where i.id=? and i.status='PUBLISHED' and s.status='APPROVED'",Integer.class,item)==0)throw new BusinessException("服务不可查看");
        var grade=forItem(item);var entries=db.query("select u.nickname,r.rating,r.content,r.created_at from reviews r join users u on u.id=r.reviewer_id where r.rated_item_id=? order by r.created_at desc,r.id desc limit 10 offset ?",(r,n)->new Entry(r.getString(1),r.getInt(2),r.getString(3),r.getTimestamp(4).toLocalDateTime()),item,page*10);return new Feedback(grade.score(),grade.count(),entries);
    }
}
