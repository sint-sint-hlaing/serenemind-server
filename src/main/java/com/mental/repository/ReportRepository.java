package com.mental.repository;

import com.mental.model.entity.Report;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends CrudRepository<Report, Long> {
    @Query("""
        SELECT COUNT(r)
        FROM Report r
        WHERE DATE(r.createdAt)=CURRENT_DATE
    """)
    long countTodayReports();
}
