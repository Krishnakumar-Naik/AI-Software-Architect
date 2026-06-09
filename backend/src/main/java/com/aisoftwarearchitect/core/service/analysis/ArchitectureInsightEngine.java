package com.aisoftwarearchitect.core.service.analysis;

import com.aisoftwarearchitect.core.domain.ClassMetadata;
import com.aisoftwarearchitect.core.domain.DependencyMetadata;
import com.aisoftwarearchitect.core.domain.MethodMetadata;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ArchitectureInsightEngine {

    public static class Insights {
        public int healthScore;
        public String technicalDebt; // LOW, MEDIUM, HIGH
        public List<List<String>> circularDependencies;
        public String mostConnectedComponent;
        public int mostConnectedComponentDegree;
        public String largestModule;
        public int largestModuleSize;
        public int layeringViolationsCount;
        public String layeredArchitectureType;
        public List<String> healthScoreFactors;
        public int architectureScore;
        public String technicalDebtScore;
        public double couplingScore;
        public double complexityScore;
    }

    public Insights calculateInsights(List<ClassMetadata> classes, List<DependencyMetadata> dependencies) {
        return calculateInsights(classes, dependencies, Collections.emptyList());
    }

    public Insights calculateInsights(List<ClassMetadata> classes, List<DependencyMetadata> dependencies, List<MethodMetadata> methods) {
        Insights insights = new Insights();
        if (classes == null || classes.isEmpty()) {
            insights.healthScore = 100;
            insights.technicalDebt = "LOW";
            insights.circularDependencies = new ArrayList<>();
            insights.mostConnectedComponent = "None";
            insights.mostConnectedComponentDegree = 0;
            insights.largestModule = "None";
            insights.largestModuleSize = 0;
            insights.layeringViolationsCount = 0;
            insights.layeredArchitectureType = "Flat / Unstructured Layout";
            insights.healthScoreFactors = Arrays.asList(
                "Circular Dependencies: No cycles detected (+0)",
                "Layer Violations: Clean layers (+0)",
                "Coupling: Optimal coupling (+0)",
                "Package Balance: Balanced (+0)",
                "Endpoint Complexity: Low complexity (+0)"
            );
            insights.architectureScore = 100;
            insights.technicalDebtScore = "LOW";
            insights.couplingScore = 0.0;
            insights.complexityScore = 0.0;
            return insights;
        }

        // 1. Stereotypes and Layer Violations
        Map<String, String> stereotypeMap = classes.stream()
                .collect(Collectors.toMap(ClassMetadata::getName, ClassMetadata::getStereotype, (a, b) -> a));

        int violationsCount = 0;
        for (DependencyMetadata dep : dependencies) {
            String srcStereo = stereotypeMap.get(dep.getSourceNode());
            String tgtStereo = stereotypeMap.get(dep.getTargetNode());

            if (srcStereo == null || tgtStereo == null) continue;

            if ("REPOSITORY".equals(srcStereo) && ("CONTROLLER".equals(tgtStereo) || "SERVICE".equals(tgtStereo))) {
                violationsCount++;
            }
            if ("SERVICE".equals(srcStereo) && "CONTROLLER".equals(tgtStereo)) {
                violationsCount++;
            }
        }
        insights.layeringViolationsCount = violationsCount;

        long controllers = classes.stream().filter(c -> "CONTROLLER".equalsIgnoreCase(c.getStereotype())).count();
        long services = classes.stream().filter(c -> "SERVICE".equalsIgnoreCase(c.getStereotype())).count();
        long repositories = classes.stream().filter(c -> "REPOSITORY".equalsIgnoreCase(c.getStereotype())).count();

        if (controllers == 0 && services == 0 && repositories == 0) {
            insights.layeredArchitectureType = "Flat / Unstructured Layout";
        } else if (violationsCount == 0) {
            insights.layeredArchitectureType = "Strict Clean Architecture";
        } else {
            insights.layeredArchitectureType = "Layer Violations Detected";
        }

        // 2. Circular Dependencies (DFS cycle check)
        List<String> nodeNames = classes.stream().map(ClassMetadata::getName).collect(Collectors.toList());
        Map<String, List<String>> adjList = new HashMap<>();
        for (String node : nodeNames) {
            adjList.put(node, new ArrayList<>());
        }
        for (DependencyMetadata dep : dependencies) {
            if (adjList.containsKey(dep.getSourceNode()) && adjList.containsKey(dep.getTargetNode())) {
                adjList.get(dep.getSourceNode()).add(dep.getTargetNode());
            }
        }

        List<List<String>> cycles = findCycles(nodeNames, adjList);
        insights.circularDependencies = cycles;

        // 3. Most Connected Component (in-degree + out-degree)
        Map<String, Integer> degrees = new HashMap<>();
        for (String node : nodeNames) {
            degrees.put(node, 0);
        }
        for (DependencyMetadata dep : dependencies) {
            if (degrees.containsKey(dep.getSourceNode())) {
                degrees.put(dep.getSourceNode(), degrees.get(dep.getSourceNode()) + 1);
            }
            if (degrees.containsKey(dep.getTargetNode())) {
                degrees.put(dep.getTargetNode(), degrees.get(dep.getTargetNode()) + 1);
            }
        }

        String maxNode = "None";
        int maxDegree = 0;
        for (Map.Entry<String, Integer> entry : degrees.entrySet()) {
            if (entry.getValue() > maxDegree) {
                maxDegree = entry.getValue();
                maxNode = entry.getKey();
            }
        }
        insights.mostConnectedComponent = maxNode;
        insights.mostConnectedComponentDegree = maxDegree;

        // 4. Largest Module (Package structure grouping)
        Map<String, Integer> packageSizes = new HashMap<>();
        for (ClassMetadata cm : classes) {
            String pkg = cm.getPackageName();
            if (pkg == null || pkg.trim().isEmpty()) {
                pkg = "default";
            }
            packageSizes.put(pkg, packageSizes.getOrDefault(pkg, 0) + 1);
        }

        String maxPkg = "default";
        int maxPkgSize = 0;
        for (Map.Entry<String, Integer> entry : packageSizes.entrySet()) {
            if (entry.getValue() > maxPkgSize) {
                maxPkgSize = entry.getValue();
                maxPkg = entry.getKey();
            }
        }
        insights.largestModule = maxPkg;
        insights.largestModuleSize = maxPkgSize;

        // Calculate factors and deductions for health score
        List<String> factors = new ArrayList<>();
        int score = 100;

        // Factor 1: Circular Dependencies
        int cyclePenalty = Math.min(cycles.size() * 15, 45);
        score -= cyclePenalty;
        factors.add("Circular Dependencies: " + (cycles.isEmpty() ? "No cycles detected (+0)" : cycles.size() + " loops detected (-" + cyclePenalty + ")"));

        // Factor 2: Layer Violations
        int violationPenalty = Math.min(violationsCount * 5, 25);
        score -= violationPenalty;
        factors.add("Layer Violations: " + (violationsCount == 0 ? "Clean layers (+0)" : violationsCount + " violations detected (-" + violationPenalty + ")"));

        // Factor 3: Coupling Density
        double couplingDensity = classes.isEmpty() ? 0.0 : (double) dependencies.size() / classes.size();
        int couplingPenalty = 0;
        if (couplingDensity > 4.0) {
            couplingPenalty = 15;
        } else if (couplingDensity > 2.0) {
            couplingPenalty = 8;
        }
        score -= couplingPenalty;
        factors.add("Coupling: " + (couplingPenalty == 0 ? "Optimal coupling density (+0)" : String.format("%.2f links/class (-%d)", couplingDensity, couplingPenalty)));

        // Factor 4: Package Balance
        double largestPkgRatio = classes.isEmpty() ? 0.0 : (double) maxPkgSize / classes.size();
        int balancePenalty = 0;
        if (largestPkgRatio > 0.6 && classes.size() > 5) {
            balancePenalty = 15;
        }
        score -= balancePenalty;
        factors.add("Package Balance: " + (balancePenalty == 0 ? "Balanced modular layout (+0)" : String.format("%.1f%% dominant package (-%d)", largestPkgRatio * 100, balancePenalty)));

        // Factor 5: Endpoint Complexity
        long endpointCount = (methods == null) ? 0 : methods.stream().filter(m -> m.getIsApiEndpoint() != null && m.getIsApiEndpoint()).count();
        long controllerCount = classes.stream().filter(c -> "CONTROLLER".equalsIgnoreCase(c.getStereotype())).count();
        double avgEndpointsPerController = controllerCount == 0 ? 0.0 : (double) endpointCount / controllerCount;
        int endpointPenalty = 0;
        if (avgEndpointsPerController > 10.0) {
            endpointPenalty = 15;
        } else if (avgEndpointsPerController > 6.0) {
            endpointPenalty = 8;
        }
        score -= endpointPenalty;
        factors.add("Endpoint Complexity: " + (endpointPenalty == 0 ? "Low endpoint density (+0)" : String.format("%.1f avg per controller (-%d)", avgEndpointsPerController, endpointPenalty)));

        insights.healthScore = Math.max(0, Math.min(score, 100));
        insights.healthScoreFactors = factors;
        insights.architectureScore = insights.healthScore;

        // Calculate Technical Debt based on:
        // - Long classes (lines of code > 300)
        // - Dependency count
        // - Circular references
        // - Large modules
        long longClassesCount = classes.stream().filter(cm -> {
            if (cm.getSourceFile() == null || cm.getSourceFile().getContent() == null) return false;
            String content = cm.getSourceFile().getContent();
            return content.split("\\r?\\n").length > 300;
        }).count();

        int cycleCount = cycles.size();
        int dependencyCount = dependencies.size();

        if (cycleCount > 2 || longClassesCount > 3 || dependencyCount > 80 || maxPkgSize > 15) {
            insights.technicalDebt = "HIGH";
        } else if (cycleCount > 0 || longClassesCount > 0 || dependencyCount > 30 || maxPkgSize > 8) {
            insights.technicalDebt = "MEDIUM";
        } else {
            insights.technicalDebt = "LOW";
        }
        insights.technicalDebtScore = insights.technicalDebt;

        // Set Coupling and Complexity scores
        insights.couplingScore = couplingDensity;
        insights.complexityScore = avgEndpointsPerController;
        return insights;
    }

    private List<List<String>> findCycles(List<String> nodes, Map<String, List<String>> adjList) {
        List<List<String>> cycles = new ArrayList<>();
        Map<String, Integer> visited = new HashMap<>(); // 0: unvisited, 1: visiting, 2: visited
        for (String node : nodes) {
            visited.put(node, 0);
        }

        List<String> path = new ArrayList<>();
        for (String node : nodes) {
            if (visited.get(node) == 0) {
                dfsCycleCheck(node, adjList, visited, path, cycles);
            }
        }
        return cycles;
    }

    private void dfsCycleCheck(String u, Map<String, List<String>> adjList, 
                               Map<String, Integer> visited, List<String> path, 
                               List<List<String>> cycles) {
        visited.put(u, 1);
        path.add(u);

        List<String> neighbors = adjList.getOrDefault(u, Collections.emptyList());
        for (String v : neighbors) {
            if (visited.containsKey(v)) {
                if (visited.get(v) == 1) {
                    int index = path.indexOf(v);
                    if (index != -1) {
                        List<String> cycle = new ArrayList<>(path.subList(index, path.size()));
                        cycle.add(v); // Visual completion: A -> B -> A
                        cycles.add(cycle);
                    }
                } else if (visited.get(v) == 0) {
                    dfsCycleCheck(v, adjList, visited, path, cycles);
                }
            }
        }

        path.remove(path.size() - 1);
        visited.put(u, 2);
    }
}
