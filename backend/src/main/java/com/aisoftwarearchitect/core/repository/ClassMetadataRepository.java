package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.ClassMetadata;
import com.aisoftwarearchitect.core.domain.SourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClassMetadataRepository extends JpaRepository<ClassMetadata, Long> {
    List<ClassMetadata> findBySourceFile(SourceFile sourceFile);
    
    @Query("SELECT c FROM ClassMetadata c LEFT JOIN FETCH c.sourceFile WHERE c.sourceFile.analysis = :analysis")
    List<ClassMetadata> findByAnalysis(@Param("analysis") Analysis analysis);
}
