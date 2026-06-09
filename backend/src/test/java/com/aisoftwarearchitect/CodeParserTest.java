package com.aisoftwarearchitect;

import com.aisoftwarearchitect.core.service.parser.CodeParser;
import com.aisoftwarearchitect.core.service.parser.ParsedClass;
import com.aisoftwarearchitect.core.service.parser.ParsedMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CodeParserTest {

    private final CodeParser parser = new CodeParser();

    @Test
    void testParseJava_Success() {
        String content = "package com.example.controller;\n" +
                "import com.example.service.MyService;\n" +
                "@RestController\n" +
                "@RequestMapping(\"/api/v1\")\n" +
                "public class UserController {\n" +
                "    private final MyService service;\n" +
                "    @GetMapping(\"/users\")\n" +
                "    public List<User> getUsers() {\n" +
                "        return service.getAll();\n" +
                "    }\n" +
                "    public void helper() {}\n" +
                "}";

        List<ParsedClass> classes = parser.parseFile(content, "JAVA");
        
        assertEquals(1, classes.size());
        ParsedClass pc = classes.get(0);
        assertEquals("UserController", pc.getName());
        assertEquals("CLASS", pc.getType());
        assertEquals("CONTROLLER", pc.getStereotype());
        assertEquals("com.example.controller", pc.getPackageName());
        
        assertTrue(pc.getDependencies().contains("MyService"));
        
        assertEquals(2, pc.getMethods().size()); // 1 for getUsers API, 1 fallback check
        ParsedMethod pm = pc.getMethods().stream()
                .filter(ParsedMethod::isApiEndpoint)
                .findFirst()
                .orElse(null);
                
        assertNotNull(pm);
        assertEquals("getUsers", pm.getName());
        assertTrue(pm.isApiEndpoint());
        assertEquals("GET", pm.getHttpMethod());
        assertEquals("/api/v1/users", pm.getPath());
    }

    @Test
    void testParsePython_Success() {
        String content = "from services import UserService\n" +
                "class AuthController:\n" +
                "    def __init__(self):\n" +
                "        self.svc = UserService()\n" +
                "    @app.route('/login')\n" +
                "    def login(self):\n" +
                "        pass\n" +
                "    def helper(self):\n" +
                "        pass";

        List<ParsedClass> classes = parser.parseFile(content, "PYTHON");

        assertEquals(1, classes.size());
        ParsedClass pc = classes.get(0);
        assertEquals("AuthController", pc.getName());
        assertEquals("CONTROLLER", pc.getStereotype());
        
        // login (API) and helper/init functions
        assertTrue(pc.getMethods().size() >= 2);
    }
}
