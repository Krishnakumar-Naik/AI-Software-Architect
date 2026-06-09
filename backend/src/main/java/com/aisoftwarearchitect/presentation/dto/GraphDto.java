package com.aisoftwarearchitect.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphDto {
    private List<NodeDataWrapper> nodes;
    private List<EdgeDataWrapper> edges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeDataWrapper {
        private NodeData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NodeData {
        private String id;
        private String label;
        private String type; // CLASS, INTERFACE, CONTROLLER, SERVICE, REPOSITORY, ENDPOINT
        private Map<String, Object> metadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EdgeDataWrapper {
        private EdgeData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EdgeData {
        private String id;
        private String source;
        private String target;
        private String relationshipType; // DEPENDS_ON, CALLS, IMPLEMENTS, EXTENDS, USES, EXPOSES_ENDPOINT
    }
}
