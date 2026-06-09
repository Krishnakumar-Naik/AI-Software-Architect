package com.aisoftwarearchitect.core.service.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedMethod {
    private String name;
    private String returnType;
    private boolean isApiEndpoint;
    private String httpMethod;
    private String path;
}
