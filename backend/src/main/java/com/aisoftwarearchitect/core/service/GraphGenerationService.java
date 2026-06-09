package com.aisoftwarearchitect.core.service;

import com.aisoftwarearchitect.core.domain.*;
import com.aisoftwarearchitect.core.repository.*;
import com.aisoftwarearchitect.presentation.dto.GraphDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphGenerationService {

    private final ClassMetadataRepository classMetadataRepository;
    private final MethodMetadataRepository methodMetadataRepository;
    private final DependencyMetadataRepository dependencyMetadataRepository;

    public GraphDto generateGraph(Analysis analysis) {
        log.info("Generating dynamic graph data for Analysis ID: {}", analysis.getId());

        List<ClassMetadata> classes = classMetadataRepository.findByAnalysis(analysis);
        List<MethodMetadata> methods = methodMetadataRepository.findByAnalysis(analysis);
        List<DependencyMetadata> dependencies = dependencyMetadataRepository.findByAnalysis(analysis);

        Map<String, ClassMetadata> classMap = classes.stream()
                .collect(Collectors.toMap(ClassMetadata::getName, c -> c, (a, b) -> a));

        List<GraphDto.NodeDataWrapper> nodes = new ArrayList<>();
        List<GraphDto.EdgeDataWrapper> edges = new ArrayList<>();

        // Group methods by class ID for quick metadata insertion
        Map<Long, List<MethodMetadata>> classMethodsMap = methods.stream()
                .collect(Collectors.groupingBy(m -> m.getClassMetadata().getId()));

        // Calculate degrees for nodes (in-degree + out-degree)
        Map<String, Integer> degreeMap = new HashMap<>();
        for (ClassMetadata cm : classes) {
            degreeMap.put(cm.getName(), 0);
        }
        for (DependencyMetadata dm : dependencies) {
            if (degreeMap.containsKey(dm.getSourceNode())) {
                degreeMap.put(dm.getSourceNode(), degreeMap.get(dm.getSourceNode()) + 1);
            }
            if (degreeMap.containsKey(dm.getTargetNode())) {
                degreeMap.put(dm.getTargetNode(), degreeMap.get(dm.getTargetNode()) + 1);
            }
        }

        // 1. Generate Class/Interface nodes
        for (ClassMetadata cm : classes) {
            List<MethodMetadata> classMethods = classMethodsMap.getOrDefault(cm.getId(), Collections.emptyList());
            List<String> methodNames = classMethods.stream().map(MethodMetadata::getName).collect(Collectors.toList());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("package", cm.getPackageName() == null ? "" : cm.getPackageName());
            metadata.put("stereotype", cm.getStereotype());
            metadata.put("methods", methodNames);
            metadata.put("degree", degreeMap.getOrDefault(cm.getName(), 0));

            nodes.add(GraphDto.NodeDataWrapper.builder()
                    .data(GraphDto.NodeData.builder()
                            .id(cm.getName())
                            .label(cm.getName())
                            .type(cm.getStereotype().equals("OTHER") ? cm.getType() : cm.getStereotype())
                            .metadata(metadata)
                            .build())
                    .build());
        }

        // 2. Generate Dependency Edges
        int edgeCounter = 1;
        for (DependencyMetadata dm : dependencies) {
            String srcName = dm.getSourceNode();
            String tgtName = dm.getTargetNode();

            ClassMetadata srcClass = classMap.get(srcName);
            ClassMetadata tgtClass = classMap.get(tgtName);

            String relationship = dm.getType(); // DEPENDS_ON, EXTENDS, IMPLEMENTS
            if ("DEPENDS_ON".equals(relationship) && srcClass != null && tgtClass != null) {
                String srcStereo = srcClass.getStereotype();
                String tgtStereo = tgtClass.getStereotype();

                if ("CONTROLLER".equals(srcStereo) && "SERVICE".equals(tgtStereo)) {
                    relationship = "CALLS";
                } else if ("SERVICE".equals(srcStereo) && "REPOSITORY".equals(tgtStereo)) {
                    relationship = "CALLS";
                } else {
                    relationship = "USES";
                }
            }

            edges.add(GraphDto.EdgeDataWrapper.builder()
                    .data(GraphDto.EdgeData.builder()
                            .id("e" + (edgeCounter++))
                            .source(srcName)
                            .target(tgtName)
                            .relationshipType(relationship)
                            .build())
                    .build());
        }

        // 3. Generate API Endpoint Nodes & exposure edges
        for (MethodMetadata mm : methods) {
            if (mm.getIsApiEndpoint()) {
                String httpMethod = mm.getHttpMethod() == null ? "GET" : mm.getHttpMethod();
                String path = mm.getPath() == null ? "/" : mm.getPath();
                String endpointNodeId = "[" + httpMethod + "]" + path;

                // Only add the endpoint node if it hasn't been added yet (unique endpoints)
                boolean endpointExists = nodes.stream()
                        .anyMatch(n -> n.getData().getId().equals(endpointNodeId));

                if (!endpointExists) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("path", path);
                    metadata.put("httpMethod", httpMethod);
                    metadata.put("controller", mm.getClassMetadata().getName());

                    nodes.add(GraphDto.NodeDataWrapper.builder()
                            .data(GraphDto.NodeData.builder()
                                    .id(endpointNodeId)
                                    .label(httpMethod + " " + path)
                                    .type("ENDPOINT")
                                    .metadata(metadata)
                                    .build())
                            .build());
                }

                // Add an edge from Controller class to the API endpoint
                edges.add(GraphDto.EdgeDataWrapper.builder()
                        .data(GraphDto.EdgeData.builder()
                                .id("e" + (edgeCounter++))
                                .source(mm.getClassMetadata().getName())
                                .target(endpointNodeId)
                                .relationshipType("EXPOSES_ENDPOINT")
                                .build())
                        .build());
            }
        }

        return GraphDto.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }
}
