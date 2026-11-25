package com.campushub.orders;

import com.campushub.common.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class OrderFeedback {
    private final JdbcTemplate db;
    private final OrderDeskService orders;
    public OrderFeedback(JdbcTemplate db, OrderDeskService orders){this.db=db;this.orders=orders;}

    public void review(long actor,String kind,long id,int rating,String content){
        var target=orders.detail(actor,kind,id); var r=target.record();
        if(!r.canReview())throw new BusinessException(r.reviewed()?"你已评价过这笔订单":"完成订单后可以评价");
        if(rating<1||rating>5||content!=null&&content.length()>1000)throw new BusinessException("评分为 1 至 5 星，评价最多 1000 字");
        Long item=(kind.equals("BOOKING")&&r.role().equals("CUSTOMER"))?r.objectId():null;
        try {db.update("insert into reviews(reviewer_id,target_user_id,target_type,target_id,rating,content,rated_item_id) values(?,?,?,?,?,?,?)",actor,target.peer(),"DESK_"+kind,id,rating,content==null?null:content.trim(),item);}
        catch(DuplicateKeyException e){throw new BusinessException("你已评价过这笔订单");}
    }
    public void complain(long actor,String kind,long id,String reason,String description){
        var row=orders.get(actor,kind,id);
        if(!row.canComplain())throw new BusinessException(row.complained()?"投诉已提交":"只可投诉本人已完成的订单");
        insertReport(actor,"DESK_"+kind,id,reason,description,actor+":"+kind+":"+id);
    }
    public void insertReport(long actor,String type,long id,String reason,String description,String key){
        if(reason==null||reason.isBlank()||reason.trim().length()>120||description!=null&&description.length()>1000)throw new BusinessException("请填写原因，说明最多 1000 字");
        try {db.update("insert into report_records(reporter_id,target_type,target_id,reason,description,status,order_dedupe_key) values(?,?,?,?,?,'OPEN',?)",actor,type,id,reason.trim(),description,key);}
        catch(DuplicateKeyException e){throw new BusinessException("已提交，请在信用中心查看进度");}
    }
}
