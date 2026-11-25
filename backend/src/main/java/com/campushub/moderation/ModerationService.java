package com.campushub.moderation;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationService {

    private final ReportRecordRepository reportRecordRepository;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    private final com.campushub.orders.OrderFeedback feedback;

    public ModerationService(ReportRecordRepository reportRecordRepository, org.springframework.jdbc.core.JdbcTemplate db, com.campushub.orders.OrderFeedback feedback) {
        this.db=db; this.feedback=feedback;
        this.reportRecordRepository = reportRecordRepository;

    }

    @Transactional
    public ReportRecordSummary report(Long reporterId, ReportRequest request) {
        String query = switch (request.targetType()) {
            case "SHOP" -> "select owner_id from shops where id=? and status='APPROVED' for update";
            case "PROJECT_AD" -> "select publisher_id from project_ads where id=? and status='APPROVED' and (expires_at is null or expires_at>current_timestamp) for update";
            default -> throw new BusinessException("交易投诉请从已完成订单提交");
        };
        var owners=db.queryForList(query,Long.class,request.targetId());
        if(owners.isEmpty()||owners.get(0).equals(reporterId))throw new BusinessException("该内容不能举报");
        feedback.insertReport(reporterId,request.targetType(),request.targetId(),request.reason(),request.description(),reporterId+":"+request.targetType()+":"+request.targetId());
        return ReportRecordSummary.from(reportRecordRepository.findFirstByReporterIdAndTargetTypeAndTargetIdOrderByIdDesc(reporterId,request.targetType(),request.targetId()).orElseThrow());
    }
}
