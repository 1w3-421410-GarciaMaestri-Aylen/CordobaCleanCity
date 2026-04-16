package com.example.garbagereporting.repository;

import com.example.garbagereporting.model.GarbageReport;
import com.example.garbagereporting.model.ReportStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GarbageReportRepository extends MongoRepository<GarbageReport, String> {

    List<GarbageReport> findByStatus(ReportStatus status);
}
