package com.intern.trustai.service;

import com.intern.trustai.dto.ReportHistoryDTO;
import java.util.List;

public interface GuardService {
    List<ReportHistoryDTO> getReportHistory();
}
