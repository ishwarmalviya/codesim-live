package com.codesim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Executes code via a self-hosted Judge0 CE instance (Docker).
 * Default URL: http://localhost:2358 — no RapidAPI or external API required.
 */
@Service
public class Judge0ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(Judge0ExecutionService.class);

    /** C is compiled as C++ so vector/string harnesses work for both. */
    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "c", 54, "cpp", 54, "c++", 54, "java", 62, "javascript", 63, "js", 63, "python", 71
    );

    @Value("${app.judge0.api-url:http://localhost:2358}")
    private String apiUrl;

    @Value("${app.judge0.auth-token:}")
    private String authToken;

    @Value("${app.judge0.auth-header:X-Auth-Token}")
    private String authHeader;

    @Value("${app.judge0.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper;
    private RestClient client;

    public Judge0ExecutionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @jakarta.annotation.PostConstruct
    void init() {
        if (apiUrl != null && !apiUrl.isBlank()) {
            var factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(3));
            factory.setReadTimeout(Duration.ofSeconds(25));
            this.client = RestClient.builder()
                    .baseUrl(apiUrl.replaceAll("/$", ""))
                    .requestFactory(factory)
                    .build();
            log.info("Judge0 CE configured at {} (enabled={})", apiUrl, enabled);
        } else {
            log.warn("Judge0 API URL not set — multi-language execution disabled");
        }
    }

    public boolean isAvailable() {
        return enabled && client != null && apiUrl != null && !apiUrl.isBlank();
    }

    public int languageId(String language) {
        if (language == null) return 71;
        return LANGUAGE_IDS.getOrDefault(language.toLowerCase(), 71);
    }

    public boolean shouldUseJudge0(String language) {
        if (language == null) return false;
        return isAvailable() && !language.equalsIgnoreCase("python");
    }

    public record JudgeResult(String stdout, String stderr, boolean success, boolean passed) {}

    public JudgeResult runSource(String source, String language) {
        return runSource(source, language, null);
    }

    public JudgeResult runSource(String source, String language, String stdin) {
        if (!isAvailable()) {
            return new JudgeResult("", "Judge0 is not running. Start it with: docker compose up judge0-server judge0-workers -d", false, false);
        }
        try {
            var body = objectMapper.createObjectNode();
            body.put("source_code", source);
            body.put("language_id", languageId(language));
            if (stdin != null && !stdin.isBlank()) {
                body.put("stdin", stdin);
            }

            body.put("cpu_time_limit", 5);
            body.put("wall_time_limit", 10);
            var spec = client.post()
                    .uri("/submissions?base64_encoded=false&wait=true")
                    .contentType(MediaType.APPLICATION_JSON);
            if (authToken != null && !authToken.isBlank()) {
                spec = spec.header(authHeader, authToken);
            }
            JsonNode created = spec.body(body).retrieve().body(JsonNode.class);
            if (created == null) {
                return new JudgeResult("", "Empty Judge0 response", false, false);
            }
            JsonNode resp = created;
            int statusId = created.path("status").path("id").asInt(0);
            String token = created.path("token").asText("");
            if (statusId < 3 && !token.isBlank()) {
                resp = pollSubmission(token);
            }
            if (resp == null) {
                return new JudgeResult("", "Judge0 timed out waiting for result at " + apiUrl, false, false);
            }
            String stdout = textOrEmpty(resp.get("stdout"));
            String stderr = textOrEmpty(resp.get("stderr"));
            String compileOutput = textOrEmpty(resp.get("compile_output"));
            if (!compileOutput.isBlank()) {
                stderr = compileOutput + (stderr.isBlank() ? "" : "\n" + stderr);
            }
            String message = textOrEmpty(resp.get("message"));
            if (!message.isBlank() && stdout.isBlank()) {
                stderr = stderr.isBlank() ? message : stderr + "\n" + message;
            }
            String status = resp.path("status").path("description").asText("");
            boolean ok = status.equals("Accepted") || stdout.contains("Result:") || stdout.contains("PASSED");
            boolean passed = stdout.trim().equals("PASSED") || stdout.trim().startsWith("PASSED");
            if (!stderr.isBlank() && stdout.isBlank()) {
                return new JudgeResult("", stderr, false, false);
            }
            return new JudgeResult(stdout, stderr, ok, passed);
        } catch (Exception e) {
            log.warn("Judge0 execution failed: {}", e.getMessage());
            return new JudgeResult("", "Judge0 error: " + e.getMessage()
                    + " — ensure Judge0 is running at " + apiUrl, false, false);
        }
    }

    private JsonNode pollSubmission(String token) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        JsonNode latest = null;
        while (System.currentTimeMillis() < deadline) {
            var get = client.get().uri("/submissions/{token}?base64_encoded=false", token);
            if (authToken != null && !authToken.isBlank()) {
                get = get.header(authHeader, authToken);
            }
            latest = get.retrieve().body(JsonNode.class);
            int statusId = latest == null ? 0 : latest.path("status").path("id").asInt(0);
            if (statusId >= 3) {
                return latest;
            }
            Thread.sleep(120);
        }
        return latest;
    }

    public String buildSampleSource(String userCode, String language, List<Object> args, String callExpr)
            throws Exception {
        String argsJson = objectMapper.writeValueAsString(args);
        String expectedJson = "null";
        String lang = language == null ? "python" : language.toLowerCase();
        return switch (lang) {
            case "javascript", "js" -> userCode + "\nconst __args = " + argsJson + ";\n"
                    + "const __result = " + (callExpr != null && !callExpr.isBlank() ? callExpr : "solution(...__args)") + ";\n"
                    + "console.log('Result: ' + JSON.stringify(__result));";
            case "java" -> javaHarness(userCode, args, expectedJson, false);
            case "cpp", "c++", "c" -> cppHarness(userCode, args, expectedJson, false);
            default -> userCode;
        };
    }

    public String buildTestSource(String userCode, String language, JsonNode testCase) throws Exception {
        List<Object> args = new java.util.ArrayList<>();
        if (testCase.has("args")) {
            testCase.get("args").forEach(n -> args.add(objectMapper.convertValue(n, Object.class)));
        }
        Object expected = testCase.has("expected_output")
                ? objectMapper.convertValue(testCase.get("expected_output"), Object.class) : null;
        String callExpr = testCase.has("call_expr") ? testCase.get("call_expr").asText() : null;
        String expectedJson = objectMapper.writeValueAsString(expected);
        String argsJson = objectMapper.writeValueAsString(args);
        String lang = language == null ? "python" : language.toLowerCase();

        return switch (lang) {
            case "javascript", "js" -> userCode + "\nconst __args = " + argsJson + ";\n"
                    + "const __expected = " + expectedJson + ";\n"
                    + "let __result;\ntry { __result = "
                    + (callExpr != null && !callExpr.isBlank() ? callExpr : "solution(...__args)") + "; }"
                    + " catch(e) { console.log('FAILED:' + e); process.exit(0); }\n"
                    + "const __eq = JSON.stringify(__result) === JSON.stringify(__expected);\n"
                    + "console.log(__eq ? 'PASSED' : 'FAILED:' + JSON.stringify(__result));";
            case "java" -> javaHarness(userCode, args, expectedJson, true);
            case "cpp", "c++", "c" -> cppHarness(userCode, args, expectedJson, true);
            default -> userCode;
        };
    }

    public String buildBatchTestSource(String userCode, String language, List<JsonNode> testCases) throws Exception {
        String lang = language == null ? "python" : language.toLowerCase();
        return switch (lang) {
            case "javascript", "js" -> jsBatch(userCode, testCases);
            case "java" -> javaBatch(userCode, testCases);
            case "cpp", "c++", "c" -> cppBatch(userCode, testCases);
            default -> userCode;
        };
    }

    public List<Boolean> parseBatchStdout(String stdout, int expectedCount) {
        List<Boolean> out = new java.util.ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            out.add(false);
        }
        if (stdout == null) return out;
        for (String line : stdout.split("\\R")) {
            line = line.trim();
            if (!line.startsWith("CODESIM_TEST ")) continue;
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3) continue;
            try {
                int idx = Integer.parseInt(parts[1]);
                if (idx >= 0 && idx < expectedCount) {
                    out.set(idx, parts[2].startsWith("PASSED"));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private String jsBatch(String userCode, List<JsonNode> testCases) throws Exception {
        StringBuilder sb = new StringBuilder(userCode);
        sb.append("\nfunction __eq(a,b){return JSON.stringify(a)===JSON.stringify(b);}\n");
        for (int i = 0; i < testCases.size(); i++) {
            JsonNode tc = testCases.get(i);
            List<Object> args = new java.util.ArrayList<>();
            if (tc.has("args")) {
                tc.get("args").forEach(n -> args.add(objectMapper.convertValue(n, Object.class)));
            }
            Object expected = tc.has("expected_output")
                    ? objectMapper.convertValue(tc.get("expected_output"), Object.class) : null;
            String callExpr = tc.has("call_expr") ? tc.get("call_expr").asText() : null;
            String call = callExpr != null && !callExpr.isBlank() ? callExpr : "solution(...__args)";
            sb.append("try {\n")
                    .append("  const __args = ").append(objectMapper.writeValueAsString(args)).append(";\n")
                    .append("  const __expected = ").append(objectMapper.writeValueAsString(expected)).append(";\n")
                    .append("  const __result = ").append(call).append(";\n")
                    .append("  console.log('CODESIM_TEST ").append(i).append(" ' + (__eq(__result,__expected) ? 'PASSED' : 'FAILED:' + JSON.stringify(__result)));\n")
                    .append("} catch(e) { console.log('CODESIM_TEST ").append(i).append(" FAILED:' + e); }\n");
        }
        return sb.toString();
    }

    private String javaBatch(String userCode, List<JsonNode> testCases) {
        StringBuilder actions = new StringBuilder();
        for (int i = 0; i < testCases.size(); i++) {
            JsonNode tc = testCases.get(i);
            List<Object> args = new java.util.ArrayList<>();
            if (tc.has("args")) {
                tc.get("args").forEach(n -> args.add(objectMapper.convertValue(n, Object.class)));
            }
            Object expected = tc.has("expected_output")
                    ? objectMapper.convertValue(tc.get("expected_output"), Object.class) : null;
            String expectedJson;
            try {
                expectedJson = objectMapper.writeValueAsString(expected);
            } catch (Exception e) {
                expectedJson = "null";
            }
            String call = "s.solution(" + javaArgs(args) + ")";
            actions.append("    try {\n")
                    .append("      String actual").append(i).append(" = stringify(").append(call).append(");\n")
                    .append("      String expected").append(i).append(" = \"").append(escapeJava(expectedJson)).append("\";\n")
                    .append("      System.out.println(\"CODESIM_TEST ").append(i).append(" \" + (jsonEq(actual")
                    .append(i).append(", expected").append(i).append(") ? \"PASSED\" : (\"FAILED:\" + actual")
                    .append(i).append(")));\n")
                    .append("    } catch (Exception e) { System.out.println(\"CODESIM_TEST ").append(i)
                    .append(" FAILED:\" + e.getMessage()); }\n");
        }
        return """
                import java.util.*;
                import java.lang.reflect.Array;
                """ + userCode + """

                public class Main {
                  public static void main(String[] a) throws Exception {
                    Solution s = new Solution();
                """ + actions + """
                  }
                  static boolean jsonEq(String a, String b) {
                    return a != null && a.replaceAll("\\\\s+", "").equals(String.valueOf(b).replaceAll("\\\\s+", ""));
                  }
                  static String stringify(Object o) {
                    if (o == null) return "null";
                    if (o instanceof Boolean || o instanceof Byte || o instanceof Short
                        || o instanceof Integer || o instanceof Long) return String.valueOf(o);
                    if (o instanceof Float || o instanceof Double) {
                      double d = ((Number) o).doubleValue();
                      if (d == Math.rint(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
                      return String.valueOf(o);
                    }
                    if (o instanceof Character) return "\\"" + o + "\\"";
                    if (o instanceof CharSequence) return "\\"" + o.toString().replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\\"") + "\\"";
                    if (o.getClass().isArray()) {
                      int n = Array.getLength(o);
                      StringBuilder sb = new StringBuilder("[");
                      for (int i = 0; i < n; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(stringify(Array.get(o, i)));
                      }
                      return sb.append(']').toString();
                    }
                    if (o instanceof Collection) {
                      Collection c = (Collection) o;
                      StringBuilder sb = new StringBuilder("[");
                      int i = 0;
                      for (Object x : c) {
                        if (i++ > 0) sb.append(',');
                        sb.append(stringify(x));
                      }
                      return sb.append(']').toString();
                    }
                    return String.valueOf(o);
                  }
                }
                """;
    }

    private String cppBatch(String userCode, List<JsonNode> testCases) {
        StringBuilder actions = new StringBuilder();
        for (int i = 0; i < testCases.size(); i++) {
            JsonNode tc = testCases.get(i);
            List<Object> args = new java.util.ArrayList<>();
            if (tc.has("args")) {
                tc.get("args").forEach(n -> args.add(objectMapper.convertValue(n, Object.class)));
            }
            Object expected = tc.has("expected_output")
                    ? objectMapper.convertValue(tc.get("expected_output"), Object.class) : null;
            String expectedJson;
            try {
                expectedJson = objectMapper.writeValueAsString(expected);
            } catch (Exception e) {
                expectedJson = "null";
            }
            StringBuilder call = new StringBuilder("solution(");
            for (int a = 0; a < args.size(); a++) {
                if (a > 0) call.append(", ");
                call.append(toCppLiteral(args.get(a)));
            }
            call.append(')');
            actions.append("  try {\n")
                    .append("    auto __r").append(i).append(" = ").append(call).append(";\n")
                    .append("    string actual").append(i).append(" = to_json(__r").append(i).append(");\n")
                    .append("    string expected").append(i).append(" = R\"JSON(").append(expectedJson).append(")JSON\";\n")
                    .append("    cout << \"CODESIM_TEST ").append(i)
                    .append(" \" << (strip(actual").append(i).append(") == strip(expected").append(i)
                    .append(") ? \"PASSED\" : string(\"FAILED:\") + actual").append(i).append(") << endl;\n")
                    .append("  } catch (...) { cout << \"CODESIM_TEST ").append(i).append(" FAILED:exception\" << endl; }\n");
        }
        return """
                #include <bits/stdc++.h>
                using namespace std;

                string to_json(int v){ return to_string(v); }
                string to_json(long long v){ return to_string(v); }
                string to_json(unsigned v){ return to_string(v); }
                string to_json(double v){
                  if (fabs(v - rint(v)) < 1e-9) return to_string((long long)llround(v));
                  ostringstream o; o<<v; return o.str();
                }
                string to_json(bool v){ return v ? "true" : "false"; }
                string to_json(const string& v){ return string("\\"")+v+"\\""; }
                string to_json(const char* v){ return to_json(string(v ? v : "")); }
                template<typename T>
                string to_json(const vector<T>& v){
                  ostringstream o; o<<'[';
                  for(size_t i=0;i<v.size();i++){ if(i) o<<','; o<<to_json(v[i]); }
                  o<<']'; return o.str();
                }
                string strip(string s){
                  s.erase(remove_if(s.begin(), s.end(), [](unsigned char c){ return isspace(c); }), s.end());
                  return s;
                }

                """ + userCode + """

                int main(){
                """ + actions + """
                  return 0;
                }
                """;
    }

    private String javaHarness(String userCode, List<Object> args, String expectedJson, boolean compare) {
        String call = "s.solution(" + javaArgs(args) + ")";
        String action = compare
                ? "    String actual = stringify(" + call + ");\n"
                + "    String expected = \"" + escapeJava(expectedJson) + "\";\n"
                + "    System.out.println(jsonEq(actual, expected) ? \"PASSED\" : (\"FAILED:\" + actual));\n"
                : "    System.out.println(\"Result: \" + stringify(" + call + "));\n";
        return """
                import java.util.*;
                import java.lang.reflect.Array;
                """ + userCode + """

                public class Main {
                  public static void main(String[] a) throws Exception {
                    Solution s = new Solution();
                """ + action + """
                  }
                  static boolean jsonEq(String a, String b) {
                    return a != null && a.replaceAll("\\\\s+", "").equals(String.valueOf(b).replaceAll("\\\\s+", ""));
                  }
                  static String stringify(Object o) {
                    if (o == null) return "null";
                    if (o instanceof Boolean || o instanceof Byte || o instanceof Short
                        || o instanceof Integer || o instanceof Long) return String.valueOf(o);
                    if (o instanceof Float || o instanceof Double) {
                      double d = ((Number) o).doubleValue();
                      if (d == Math.rint(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
                      return String.valueOf(o);
                    }
                    if (o instanceof Character) return "\\"" + o + "\\"";
                    if (o instanceof CharSequence) return "\\"" + o.toString().replace("\\\\", "\\\\\\\\").replace("\\"", "\\\\\\"") + "\\"";
                    if (o.getClass().isArray()) {
                      int n = Array.getLength(o);
                      StringBuilder sb = new StringBuilder("[");
                      for (int i = 0; i < n; i++) {
                        if (i > 0) sb.append(',');
                        sb.append(stringify(Array.get(o, i)));
                      }
                      return sb.append(']').toString();
                    }
                    if (o instanceof Collection) {
                      Collection c = (Collection) o;
                      StringBuilder sb = new StringBuilder("[");
                      int i = 0;
                      for (Object x : c) {
                        if (i++ > 0) sb.append(',');
                        sb.append(stringify(x));
                      }
                      return sb.append(']').toString();
                    }
                    return String.valueOf(o);
                  }
                }
                """;
    }

    private String cppHarness(String userCode, List<Object> args, String expectedJson, boolean compare) {
        StringBuilder call = new StringBuilder("solution(");
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(toCppLiteral(args.get(i)));
        }
        call.append(')');
        String body = compare
                ? "  auto __r = " + call + ";\n"
                + "  string actual = to_json(__r);\n"
                + "  string expected = R\"JSON(" + expectedJson + ")JSON\";\n"
                + "  auto strip = [](string s){ s.erase(remove_if(s.begin(), s.end(), [](unsigned char c){ return isspace(c); }), s.end()); return s; };\n"
                + "  cout << (strip(actual) == strip(expected) ? \"PASSED\" : string(\"FAILED:\") + actual) << endl;\n"
                : "  cout << \"Result: \" << to_json(" + call + ") << endl;\n";
        return """
                #include <bits/stdc++.h>
                using namespace std;

                string to_json(int v){ return to_string(v); }
                string to_json(long long v){ return to_string(v); }
                string to_json(unsigned v){ return to_string(v); }
                string to_json(double v){
                  if (fabs(v - rint(v)) < 1e-9) return to_string((long long)llround(v));
                  ostringstream o; o<<v; return o.str();
                }
                string to_json(bool v){ return v ? "true" : "false"; }
                string to_json(const string& v){ return string("\\"")+v+"\\""; }
                string to_json(const char* v){ return to_json(string(v ? v : "")); }
                template<typename T>
                string to_json(const vector<T>& v){
                  ostringstream o; o<<'[';
                  for(size_t i=0;i<v.size();i++){ if(i) o<<','; o<<to_json(v[i]); }
                  o<<']'; return o.str();
                }

                """ + userCode + """

                int main(){
                """ + body + """
                  return 0;
                }
                """;
    }

    private String javaArgs(List<Object> args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(toJavaLiteral(args.get(i)));
        }
        return sb.toString();
    }

    private String toJavaLiteral(Object val) {
        if (val == null) return "null";
        if (val instanceof Integer || val instanceof Short || val instanceof Byte) return val.toString();
        if (val instanceof Long) return val + "L";
        if (val instanceof Double || val instanceof Float) {
            double d = ((Number) val).doubleValue();
            if (d == Math.rint(d)) return String.valueOf((long) d);
            return val.toString() + (val instanceof Float ? "f" : "");
        }
        if (val instanceof Boolean) return val.toString();
        if (val instanceof String s) return "\"" + escapeJava(s) + "\"";
        if (val instanceof List<?> list) {
            if (list.isEmpty()) return "new int[]{}";
            Object first = list.get(0);
            if (first instanceof List<?>) {
                StringBuilder sb = new StringBuilder("new int[][]{");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(toJavaLiteral(list.get(i)));
                }
                return sb.append('}').toString();
            }
            if (first instanceof String) {
                StringBuilder sb = new StringBuilder("new String[]{");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(toJavaLiteral(list.get(i)));
                }
                return sb.append('}').toString();
            }
            StringBuilder sb = new StringBuilder("new int[]{");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJavaLiteral(list.get(i)));
            }
            return sb.append('}').toString();
        }
        return "null";
    }

    private String toCppLiteral(Object val) {
        if (val == null) return "0";
        if (val instanceof Integer || val instanceof Short || val instanceof Byte || val instanceof Long)
            return val.toString();
        if (val instanceof Double || val instanceof Float) {
            double d = ((Number) val).doubleValue();
            if (d == Math.rint(d)) return String.valueOf((long) d);
            return val.toString();
        }
        if (val instanceof Boolean) return (Boolean) val ? "true" : "false";
        if (val instanceof String s) return "string(\"" + escapeJava(s) + "\")";
        if (val instanceof List<?> list) {
            if (list.isEmpty()) return "vector<int>{}";
            Object first = list.get(0);
            if (first instanceof List<?>) {
                StringBuilder sb = new StringBuilder("vector<vector<int>>{");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(toCppLiteral(list.get(i)));
                }
                return sb.append('}').toString();
            }
            if (first instanceof String) {
                StringBuilder sb = new StringBuilder("vector<string>{");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(toCppLiteral(list.get(i)));
                }
                return sb.append('}').toString();
            }
            StringBuilder sb = new StringBuilder("vector<int>{");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toCppLiteral(list.get(i)));
            }
            return sb.append('}').toString();
        }
        return "0";
    }

    private static String escapeJava(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.asText("");
    }
}
