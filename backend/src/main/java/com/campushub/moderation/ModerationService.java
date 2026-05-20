package com.campushub.moderation;

import com.campushub.common.BusinessException;
import com.campushub.user.User;
import com.campushub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationService {

    private final ReportRecordRepository reportRecordRepository;
    private final UserRepository userRepository;

    public ModerationService(ReportRecordRepository reportRecordRepository, UserRepository userRepository) {
        this.reportRecordRepository = reportRecordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReportRecordSummary report(Long reporterId, ReportRequest request) {
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new BusinessException("举报人不存在"));
        ReportRecord report = new ReportRecord(reporter, request.targetType(), request.targetId(), request.reason(), request.description());
        return ReportRecordSummary.from(reportRecordRepository.save(report));
    }
}
