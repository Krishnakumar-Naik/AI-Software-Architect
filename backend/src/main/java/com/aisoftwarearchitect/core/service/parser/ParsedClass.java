package com.aisoftwarearchitect.core.service.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedClass {
    private String name;
    private String type; // CLASS, INTERFACE
    private String stereotype; // CONTROLLER, SERVICE, REPOSITORY, OTHER
    private String packageName;
    @Builder.Default
    private List<ParsedMethod> methods = new ArrayList<>();
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();
}
