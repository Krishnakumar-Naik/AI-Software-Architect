package com.aisoftwarearchitect.core.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CodeParser {

    public List<ParsedClass> parseFile(String content, String fileType) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        switch (fileType.toUpperCase()) {
            case "JAVA":
                return parseJava(content);
            case "PYTHON":
                return parsePython(content);
            case "JAVASCRIPT":
            case "TYPESCRIPT":
                return parseJsTs(content);
            default:
                return new ArrayList<>();
        }
    }

    private List<ParsedClass> parseJava(String content) {
        List<ParsedClass> classes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            String currentPackage = "";
            String activeStereotype = "OTHER";
            List<String> imports = new ArrayList<>();
            List<ParsedMethod> currentMethods = new ArrayList<>();
            
            String activeClassName = null;
            String activeClassType = "CLASS";
            String controllerBasePath = "";

            Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w\\.]+)\\s*;");
            Pattern importPattern = Pattern.compile("^\\s*import\\s+([\\w\\.]+)\\s*;");
            Pattern classPattern = Pattern.compile("public\\s+(?:final\\s+)?(?:class|interface|enum)\\s+(\\w+)");
            Pattern interfacePattern = Pattern.compile("public\\s+interface\\s+(\\w+)");
            Pattern controllerMappingPattern = Pattern.compile("@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
            Pattern apiMappingPattern = Pattern.compile("@(?:Request|Get|Post|Put|Delete)Mapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");
            Pattern methodPattern = Pattern.compile("(?:public|protected|private|static|\\s)+\\s+([\\w\\<\\>\\?]+)\\s+(\\w+)\\s*\\(");
            
            Pattern getMapping = Pattern.compile("@GetMapping");
            Pattern postMapping = Pattern.compile("@PostMapping");
            Pattern putMapping = Pattern.compile("@PutMapping");
            Pattern deleteMapping = Pattern.compile("@DeleteMapping");

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                Matcher pm = packagePattern.matcher(trimmed);
                if (pm.find()) {
                    currentPackage = pm.group(1);
                    continue;
                }

                Matcher im = importPattern.matcher(trimmed);
                if (im.find()) {
                    String importedClass = im.group(1);
                    String[] parts = importedClass.split("\\.");
                    if (parts.length > 0) {
                        imports.add(parts[parts.length - 1]);
                    }
                    continue;
                }

                if (trimmed.contains("@RestController") || trimmed.contains("@Controller")) {
                    activeStereotype = "CONTROLLER";
                } else if (trimmed.contains("@Service")) {
                    activeStereotype = "SERVICE";
                } else if (trimmed.contains("@Repository")) {
                    activeStereotype = "REPOSITORY";
                }

                Matcher rmm = controllerMappingPattern.matcher(trimmed);
                if (rmm.find() && activeStereotype.equals("CONTROLLER")) {
                    controllerBasePath = rmm.group(1);
                }

                Matcher cm = classPattern.matcher(trimmed);
                boolean isInterface = interfacePattern.matcher(trimmed).find();
                if (cm.find()) {
                    if (activeClassName != null) {
                        classes.add(ParsedClass.builder()
                                .name(activeClassName)
                                .type(activeClassType)
                                .packageName(currentPackage)
                                .stereotype(activeStereotype)
                                .methods(new ArrayList<>(currentMethods))
                                .dependencies(new ArrayList<>(imports))
                                .build());
                        currentMethods.clear();
                    }

                    activeClassName = cm.group(1);
                    activeClassType = isInterface ? "INTERFACE" : "CLASS";
                    continue;
                }

                boolean isApi = false;
                String httpMethod = null;
                String apiPath = "";

                if (trimmed.startsWith("@GetMapping") || getMapping.matcher(trimmed).find()) {
                    isApi = true;
                    httpMethod = "GET";
                } else if (trimmed.startsWith("@PostMapping") || postMapping.matcher(trimmed).find()) {
                    isApi = true;
                    httpMethod = "POST";
                } else if (trimmed.startsWith("@PutMapping") || putMapping.matcher(trimmed).find()) {
                    isApi = true;
                    httpMethod = "PUT";
                } else if (trimmed.startsWith("@DeleteMapping") || deleteMapping.matcher(trimmed).find()) {
                    isApi = true;
                    httpMethod = "DELETE";
                }

                if (isApi) {
                    Matcher mappingMatch = apiMappingPattern.matcher(trimmed);
                    if (mappingMatch.find()) {
                        apiPath = controllerBasePath + mappingMatch.group(1);
                    } else {
                        apiPath = controllerBasePath;
                    }
                    
                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        Matcher mmMatch = methodPattern.matcher(nextLine.trim());
                        if (mmMatch.find()) {
                            currentMethods.add(ParsedMethod.builder()
                                    .name(mmMatch.group(2))
                                    .returnType(mmMatch.group(1))
                                    .isApiEndpoint(true)
                                    .httpMethod(httpMethod)
                                    .path(apiPath)
                                    .build());
                            break;
                        }
                    }
                    continue;
                }

                Matcher mmMatch = methodPattern.matcher(trimmed);
                if (mmMatch.find() && !trimmed.contains("new ") && !trimmed.startsWith("return ") && activeClassName != null) {
                    String retType = mmMatch.group(1);
                    String mName = mmMatch.group(2);
                    if (!mName.equals("if") && !mName.equals("for") && !mName.equals("while") && !mName.equals("switch")) {
                        currentMethods.add(ParsedMethod.builder()
                                .name(mName)
                                .returnType(retType)
                                .isApiEndpoint(false)
                                .build());
                    }
                }
            }

            if (activeClassName != null) {
                classes.add(ParsedClass.builder()
                        .name(activeClassName)
                        .type(activeClassType)
                        .packageName(currentPackage)
                        .stereotype(activeStereotype)
                        .methods(new ArrayList<>(currentMethods))
                        .dependencies(new ArrayList<>(imports))
                        .build());
            }

        } catch (Exception e) {
            log.error("Error parsing Java file", e);
        }
        return classes;
    }

    private List<ParsedClass> parsePython(String content) {
        List<ParsedClass> classes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            List<ParsedMethod> currentMethods = new ArrayList<>();
            List<String> dependencies = new ArrayList<>();
            String activeClassName = null;
            String activeStereotype = "OTHER";

            Pattern classPattern = Pattern.compile("^\\s*class\\s+(\\w+)");
            Pattern defPattern = Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\(");
            Pattern importPattern = Pattern.compile("^\\s*(?:import\\s+(\\w+)|from\\s+(\\w+)\\s+import)");
            Pattern routePattern = Pattern.compile("@(?:app|router|api)\\.(get|post|put|delete|route)\\(\\s*[\"']([^\"']+)[\"']");

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                Matcher im = importPattern.matcher(trimmed);
                if (im.find()) {
                    String dep = im.group(1) != null ? im.group(1) : im.group(2);
                    dependencies.add(dep);
                    continue;
                }

                Matcher cm = classPattern.matcher(trimmed);
                if (cm.find()) {
                    if (activeClassName != null) {
                        classes.add(ParsedClass.builder()
                                .name(activeClassName)
                                .type("CLASS")
                                .packageName("")
                                .stereotype(activeStereotype)
                                .methods(new ArrayList<>(currentMethods))
                                .dependencies(new ArrayList<>(dependencies))
                                .build());
                        currentMethods.clear();
                    }
                    activeClassName = cm.group(1);
                    activeStereotype = activeClassName.endsWith("Controller") ? "CONTROLLER" :
                                      (activeClassName.endsWith("Service") ? "SERVICE" :
                                      (activeClassName.endsWith("Repository") ? "REPOSITORY" : "OTHER"));
                    continue;
                }

                Matcher rm = routePattern.matcher(trimmed);
                if (rm.find()) {
                    String httpMethod = rm.group(1).toUpperCase();
                    if (httpMethod.equals("ROUTE")) httpMethod = "GET";
                    String path = rm.group(2);

                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        Matcher dmMatch = defPattern.matcher(nextLine.trim());
                        if (dmMatch.find()) {
                            currentMethods.add(ParsedMethod.builder()
                                    .name(dmMatch.group(1))
                                    .returnType("Any")
                                    .isApiEndpoint(true)
                                    .httpMethod(httpMethod)
                                    .path(path)
                                    .build());
                            break;
                        }
                    }
                    continue;
                }

                Matcher dmMatch = defPattern.matcher(trimmed);
                if (dmMatch.find() && activeClassName != null) {
                    currentMethods.add(ParsedMethod.builder()
                            .name(dmMatch.group(1))
                            .returnType("Any")
                            .isApiEndpoint(false)
                            .build());
                }
            }

            if (activeClassName != null) {
                classes.add(ParsedClass.builder()
                        .name(activeClassName)
                        .type("CLASS")
                        .packageName("")
                        .stereotype(activeStereotype)
                        .methods(new ArrayList<>(currentMethods))
                        .dependencies(new ArrayList<>(dependencies))
                        .build());
            }

        } catch (Exception e) {
            log.error("Error parsing Python file", e);
        }
        return classes;
    }

    private List<ParsedClass> parseJsTs(String content) {
        List<ParsedClass> classes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            List<ParsedMethod> currentMethods = new ArrayList<>();
            List<String> dependencies = new ArrayList<>();
            String activeClassName = null;
            String activeStereotype = "OTHER";
            
            Pattern classPattern = Pattern.compile("^\\s*(?:export\\s+)?class\\s+(\\w+)");
            Pattern methodPattern = Pattern.compile("^\\s*(?:async\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?::|\\{)");
            Pattern importPattern = Pattern.compile("(?:import\\s+.*\\s+from\\s+|require\\()[\"']([^\"']+)[\"']");
            
            Pattern controllerPattern = Pattern.compile("@Controller\\(\\s*[\"']([^\"']*)[\"']\\s*\\)");
            Pattern routePattern = Pattern.compile("@(?:Get|Post|Put|Delete)\\(\\s*[\"']([^\"']*)[\"']\\s*\\)");
            String controllerBasePath = "";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                Matcher im = importPattern.matcher(trimmed);
                if (im.find()) {
                    String depPath = im.group(1);
                    String[] parts = depPath.split("/");
                    dependencies.add(parts[parts.length - 1]);
                    continue;
                }

                Matcher cdm = controllerPattern.matcher(trimmed);
                if (cdm.find()) {
                    activeStereotype = "CONTROLLER";
                    controllerBasePath = cdm.group(1);
                } else if (trimmed.contains("@Injectable")) {
                    activeStereotype = "SERVICE";
                }

                Matcher cm = classPattern.matcher(trimmed);
                if (cm.find()) {
                    if (activeClassName != null) {
                        classes.add(ParsedClass.builder()
                                .name(activeClassName)
                                .type("CLASS")
                                .packageName("")
                                .stereotype(activeStereotype)
                                .methods(new ArrayList<>(currentMethods))
                                .dependencies(new ArrayList<>(dependencies))
                                .build());
                        currentMethods.clear();
                    }
                    activeClassName = cm.group(1);
                    if (activeStereotype.equals("OTHER")) {
                        activeStereotype = activeClassName.endsWith("Controller") ? "CONTROLLER" :
                                          (activeClassName.endsWith("Service") ? "SERVICE" :
                                          (activeClassName.endsWith("Repository") ? "REPOSITORY" : "OTHER"));
                    }
                    continue;
                }

                Matcher rm = routePattern.matcher(trimmed);
                if (rm.find()) {
                    String httpMethod = trimmed.substring(1, trimmed.indexOf("(")).toUpperCase();
                    String path = controllerBasePath + (rm.group(1).isEmpty() ? "" : "/" + rm.group(1));

                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        Matcher mmMatch = methodPattern.matcher(nextLine.trim());
                        if (mmMatch.find()) {
                            currentMethods.add(ParsedMethod.builder()
                                    .name(mmMatch.group(1))
                                    .returnType("any")
                                    .isApiEndpoint(true)
                                    .httpMethod(httpMethod)
                                    .path(path)
                                    .build());
                            break;
                        }
                    }
                    continue;
                }

                Matcher mmMatch = methodPattern.matcher(trimmed);
                if (mmMatch.find() && activeClassName != null) {
                    String mName = mmMatch.group(1);
                    if (!mName.equals("if") && !mName.equals("for") && !mName.equals("while") && !mName.equals("switch") && !mName.equals("constructor")) {
                        currentMethods.add(ParsedMethod.builder()
                                .name(mName)
                                .returnType("any")
                                .isApiEndpoint(false)
                                .build());
                    }
                }
            }

            if (activeClassName != null) {
                classes.add(ParsedClass.builder()
                        .name(activeClassName)
                        .type("CLASS")
                        .packageName("")
                        .stereotype(activeStereotype)
                        .methods(new ArrayList<>(currentMethods))
                        .dependencies(new ArrayList<>(dependencies))
                        .build());
            }

        } catch (Exception e) {
            log.error("Error parsing JS/TS file", e);
        }
        return classes;
    }
}
