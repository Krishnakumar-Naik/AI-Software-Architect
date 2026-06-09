package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.SourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SourceFileRepository extends JpaRepository<SourceFile, Long> {
    List<SourceFile> findByAnalysis(Analysis analysis);
}
