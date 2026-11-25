package com.campushub.orders;

import com.campushub.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service @Transactional(readOnly=true)
public class OrderDeskService {
    public record Row(String kind,long id,long objectId,String title,String role,String status,String stage,
                      BigDecimal amount,LocalDateTime createdAt,long interested,boolean canReview,boolean reviewed,boolean canComplain,boolean complained) {}
    record Owned(Row record, Long peer) {}
    public record Page(List<Row> items,long total,Map<String,Long> counts) {}
    private static final Map<String,List<String>> TYPES=Map.of("TASK",List.of("TASK"),"GOODS",List.of("GOODS_LISTING","GOODS_INTENT","GOODS_ORDER"),"BOOKING",List.of("BOOKING"),"PROJECT",List.of("PROJECT"));
    private static final Set<String> STAGES=Set.of("ALL","PENDING","ACTIVE","REVIEW","ENDED");
    private final NamedParameterJdbcTemplate db;
    public OrderDeskService(NamedParameterJdbcTemplate db){this.db=db;}
    public Page list(long uid,String type,String stage,int page,int size){
        if(page<0||page>100000||size<1||size>50||!STAGES.contains(stage)||!(type.equals("ALL")||TYPES.containsKey(type)))throw new BusinessException("订单筛选无效");
        var params=new MapSqlParameterSource("uid",uid);String from=" from ("+OrderDeskQuery.RECORDS+") x where 1=1";
        if(!type.equals("ALL")){params.addValue("kinds",TYPES.get(type));from+=" and kind in (:kinds)";}
        var counts=new LinkedHashMap<String,Long>(); STAGES.forEach(k->counts.put(k,0L));
        db.query("select stage,count(*) n"+from+" group by stage",params,(ResultSet r)->{while(r.next())counts.put(r.getString("stage"),r.getLong("n"));return null;});
        counts.put("ALL",counts.values().stream().mapToLong(Long::longValue).sum());
        if(!stage.equals("ALL")){from+=" and stage=:stage";params.addValue("stage",stage);}
        params.addValue("size",size).addValue("offset",page*size);
        var items=db.query("select *"+from+" order by created_at desc,kind,rec_id desc limit :size offset :offset",params,(r,i)->map(r,uid).record());
        return new Page(items,counts.get(stage),counts);
    }
    Owned detail(long uid,String kind,long id){
        var found=db.query("select * from ("+OrderDeskQuery.RECORDS+") x where kind=:kind and rec_id=:id",Map.of("uid",uid,"kind",kind,"id",id),(r,i)->map(r,uid));
        if(found.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"订单不存在或不可查看");return found.get(0);
    }
    public Row get(long uid,String kind,long id){return detail(uid,kind,id).record();}
    private static Owned map(ResultSet r,long uid)throws SQLException{
        Number n=(Number)r.getObject("peer");Long peer=n==null?null:n.longValue();boolean done=peer!=null&&peer!=uid&&"COMPLETED".equals(r.getString("raw_status"));
        boolean reviewed=r.getBoolean("reviewed"),complained=r.getBoolean("complained");
        return new Owned(new Row(r.getString("kind"),r.getLong("rec_id"),r.getLong("object_id"),r.getString("title"),r.getString("actor_role"),r.getString("raw_status"),r.getString("stage"),r.getBigDecimal("amount"),r.getTimestamp("created_at").toLocalDateTime(),r.getLong("interested"),done&&!reviewed,reviewed,done&&!complained,complained),peer);
    }
}
