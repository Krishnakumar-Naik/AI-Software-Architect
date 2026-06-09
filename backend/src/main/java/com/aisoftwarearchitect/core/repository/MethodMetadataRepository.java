package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.MethodMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MethodMetadataRepository extends JpaRepository<MethodMetadata, Long> {
    
    @Query("SELECT m FROM MethodMetadata m WHERE m.classMetadata.sourceFile.analysis = :analysis")
    List<MethodMetadata> findByAnalysis(@Param("analysis") Analysis analysis);
}
