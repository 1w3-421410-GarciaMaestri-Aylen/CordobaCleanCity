package com.example.garbagereporting.repository;

import com.example.garbagereporting.model.Report;
import com.example.garbagereporting.model.ReportStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ReportRepository extends MongoRepository<Report, String> {

    java.util.Optional<Report> findByRequestId(String requestId);

    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByStatusInOrderByCreatedAtDesc(List<ReportStatus> statuses);

    @Query(value = "{ 'createdAt': { '$gte': ?0, '$lt': ?1 } }", sort = "{ 'createdAt': -1 }")
    List<Report> findByCreatedAtRangeOrderByCreatedAtDesc(Instant from, Instant to);

    @Query(value = "{ 'createdAt': { '$gte': ?0, '$lt': ?1 }, 'status': { '$in': ?2 } }", sort = "{ 'createdAt': -1 }")
    List<Report> findByCreatedAtRangeAndStatusInOrderByCreatedAtDesc(
            Instant from,
            Instant to,
            List<ReportStatus> statuses
    );

    List<Report> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Report> findByUserIdAndStatusInOrderByCreatedAtDesc(String userId, List<ReportStatus> statuses);

    @Query(value = "{ 'userId': ?0, 'createdAt': { '$gte': ?1, '$lt': ?2 } }", sort = "{ 'createdAt': -1 }")
    List<Report> findByUserIdAndCreatedAtRangeOrderByCreatedAtDesc(
            String userId,
            Instant from,
            Instant to
    );

    @Query(value = "{ 'userId': ?0, 'createdAt': { '$gte': ?1, '$lt': ?2 }, 'status': { '$in': ?3 } }", sort = "{ 'createdAt': -1 }")
    List<Report> findByUserIdAndCreatedAtRangeAndStatusInOrderByCreatedAtDesc(
            String userId,
            Instant from,
            Instant to,
            List<ReportStatus> statuses
    );
}
