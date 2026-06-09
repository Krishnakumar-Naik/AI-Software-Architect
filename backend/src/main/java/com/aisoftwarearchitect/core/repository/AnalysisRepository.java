package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByProjectOrderByVersionDesc(Project project);
    
    @Query("SELECT COALESCE(MAX(a.version), 0) FROM Analysis a WHERE a.project = :project")
    int findMaxVersionByProject(@Param("project") Project project);
}
