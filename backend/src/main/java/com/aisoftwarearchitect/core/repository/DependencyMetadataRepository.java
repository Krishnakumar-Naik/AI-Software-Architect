package com.aisoftwarearchitect.core.repository;

import com.aisoftwarearchitect.core.domain.Analysis;
import com.aisoftwarearchitect.core.domain.DependencyMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DependencyMetadataRepository extends JpaRepository<DependencyMetadata, Long> {
    List<DependencyMetadata> findByAnalysis(Analysis analysis);
}
