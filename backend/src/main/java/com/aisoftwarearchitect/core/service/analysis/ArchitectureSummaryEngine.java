package com.aisoftwarearchitect.core.service.analysis;

import com.aisoftwarearchitect.core.domain.ClassMetadata;
import com.aisoftwarearchitect.core.domain.DependencyMetadata;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ArchitectureSummaryEngine {

    public String generateSummary(List<ClassMetadata> classes, List<DependencyMetadata> dependencies) {
        if (classes == null || classes.isEmpty()) {
            return "# Architecture Summary\n\nNo classes found in the analysis run.";
        }

        long controllers = classes.stream().filter(c -> "CONTROLLER".equalsIgnoreCase(c.getStereotype())).count();
        long services = classes.stream().filter(c -> "SERVICE".equalsIgnoreCase(c.getStereotype())).count();
        long repositories = classes.stream().filter(c -> "REPOSITORY".equalsIgnoreCase(c.getStereotype())).count();
        long others = classes.size() - controllers - services - repositories;

        double couplingDensity = classes.isEmpty() ? 0.0 : (double) dependencies.size() / classes.size();

        Map<String, String> stereotypeMap = classes.stream()
                .collect(Collectors.toMap(ClassMetadata::getName, ClassMetadata::getStereotype, (a, b) -> a));

        StringBuilder violations = new StringBuilder();
        int violationCount = 0;

        for (DependencyMetadata dep : dependencies) {
            String srcStereo = stereotypeMap.get(dep.getSourceNode());
            String tgtStereo = stereotypeMap.get(dep.getTargetNode());

            if (srcStereo == null || tgtStereo == null) continue;

            if ("REPOSITORY".equals(srcStereo) && ("CONTROLLER".equals(tgtStereo) || "SERVICE".equals(tgtStereo))) {
                violations.append("- ⚠️ Layer Violation: Repository `").append(dep.getSourceNode())
                        .append("` depends directly on `").append(tgtStereo.toLowerCase())
                        .append("` `").append(dep.getTargetNode()).append("`.\n");
                violationCount++;
            }

            if ("SERVICE".equals(srcStereo) && "CONTROLLER".equals(tgtStereo)) {
                violations.append("- ⚠️ Layer Violation: Service `").append(dep.getSourceNode())
                        .append("` depends directly on controller `").append(dep.getTargetNode()).append("`.\n");
                violationCount++;
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("# Architectural Telemetry Report\n\n");
        
        report.append("## Structural Overview\n");
        report.append("- **Total Classes/Interfaces**: ").append(classes.size()).append("\n");
        report.append("- **Controllers**: ").append(controllers).append("\n");
        report.append("- **Services**: ").append(services).append("\n");
        report.append("- **Repositories**: ").append(repositories).append("\n");
        report.append("- **Other components**: ").append(others).append("\n");
        report.append("- **Coupling Density**: ").append(String.format("%.2f", couplingDensity)).append(" dependencies per class\n\n");

        report.append("## Layering Rules Checks\n");
        if (violationCount == 0) {
            report.append("- ✅ **Clean Architecture layering verified!** No lower-layer to higher-layer dependency violations were detected.\n\n");
        } else {
            report.append("- ❌ **").append(violationCount).append(" layering rule violation(s) detected**:\n");
            report.append(violations.toString()).append("\n");
        }

        report.append("## Coupling Analysis\n");
        if (dependencies.isEmpty()) {
            report.append("- No internal class-to-class dependencies detected. This might be a flat structure or isolated scripts.\n");
        } else {
            Map<String, Integer> dependencyCount = new HashMap<>();
            for (DependencyMetadata dep : dependencies) {
                dependencyCount.put(dep.getSourceNode(), dependencyCount.getOrDefault(dep.getSourceNode(), 0) + 1);
            }
            
            String keyNode = dependencyCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
            
            int maxDeps = dependencyCount.getOrDefault(keyNode, 0);

            report.append("- **Most Active Component**: `").append(keyNode).append("` (out-degree: ").append(maxDeps).append(" dependencies)\n");
            if (maxDeps > 5) {
                report.append("  - 💡 *Tip*: `").append(keyNode).append("` has high coupling. Consider splitting it to increase cohesion.\n");
            } else {
                report.append("  - Level of coupling is within modular parameters.\n");
            }
        }

        return report.toString();
    }
}
