package com.codesim.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final int MAX_OUTPUT_CHARS = 5000;

    @Value("${app.code-execution.python-bin}")
    private String pythonBin;

    @Value("${app.code-execution.timeout-seconds}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;
    private final Judge0ExecutionService judge0ExecutionService;
    private Path runnerScriptPath;

    public CodeExecutionService(ObjectMapper objectMapper, Judge0ExecutionService judge0ExecutionService) {
        this.objectMapper = objectMapper;
        this.judge0ExecutionService = judge0ExecutionService;
    }

    @PostConstruct
    public void init() throws IOException {
        Path tempDir = Files.createTempDirectory("codesim-runner");
        Path scriptPath = tempDir.resolve("runner.py");
        try (InputStream in = new ClassPathResource("python/runner.py").getInputStream()) {
            Files.copy(in, scriptPath);
        }
        this.runnerScriptPath = scriptPath;
        tempDir.toFile().deleteOnExit();
        scriptPath.toFile().deleteOnExit();
    }

    public record TestResult(boolean passed, String error, Object actual, String description, boolean hidden) {}

    public record TestRunSummary(List<TestResult> results, int passed, int total, boolean allPassed) {}

    public record PlainRunResult(String stdout, String stderr, boolean success) {}

    public TestRunSummary runTests(String code, List<JsonNode> testCases) {
        return runTests(code, testCases, "python");
    }

    public TestRunSummary runTests(String code, List<JsonNode> testCases, String language) {
        if (judge0ExecutionService.shouldUseJudge0(language)) {
            return runTestsViaJudge0(code, testCases, language);
        }

        if (testCases == null || testCases.isEmpty()) {
            return new TestRunSummary(List.of(), 0, 0, false);
        }

        List<TestResult> results = new ArrayList<>();
        int passedCount = 0;

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("mode", "test_all");
        payload.put("code", code);
        payload.set("test_cases", objectMapper.valueToTree(testCases));
        JsonNode out = execute(payload);

        if (out != null && out.has("results") && out.get("results").isArray()) {
            int i = 0;
            for (JsonNode row : out.get("results")) {
                JsonNode tc = i < testCases.size() ? testCases.get(i) : null;
                String description = tc != null && tc.has("description") ? tc.get("description").asText() : "Test";
                boolean hidden = tc != null && tc.has("hidden") && tc.get("hidden").asBoolean();
                boolean passed = row.path("passed").asBoolean(false);
                String error = row.hasNonNull("error") ? row.get("error").asText() : null;
                Object actual = row.has("actual") ? objectMapper.convertValue(row.get("actual"), Object.class) : null;
                if (passed) passedCount++;
                results.add(new TestResult(passed, error, actual, description, hidden));
                i++;
            }
            while (results.size() < testCases.size()) {
                JsonNode tc = testCases.get(results.size());
                String description = tc.has("description") ? tc.get("description").asText() : "Test";
                boolean hidden = tc.has("hidden") && tc.get("hidden").asBoolean();
                results.add(new TestResult(false, "No result", null, description, hidden));
            }
            return new TestRunSummary(results, passedCount, testCases.size(), passedCount == testCases.size());
        }

        for (JsonNode tc : testCases) {
            String description = tc.has("description") ? tc.get("description").asText() : "Test";
            boolean hidden = tc.has("hidden") && tc.get("hidden").asBoolean();
            results.add(new TestResult(false, out != null && out.has("error") ? out.get("error").asText() : "Execution failed", null, description, hidden));
        }
        return new TestRunSummary(results, 0, testCases.size(), false);
    }

    private TestRunSummary runTestsViaJudge0(String code, List<JsonNode> testCases, String language) {
        if (testCases == null || testCases.isEmpty()) {
            return new TestRunSummary(List.of(), 0, 0, false);
        }
        List<TestResult> results = new ArrayList<>();
        try {
            String source = judge0ExecutionService.buildBatchTestSource(code, language, testCases);
            var jr = judge0ExecutionService.runSource(source, language);
            if (!jr.stderr().isBlank() && (jr.stdout() == null || !jr.stdout().contains("CODESIM_TEST"))) {
                for (JsonNode tc : testCases) {
                    String description = tc.has("description") ? tc.get("description").asText() : "Test";
                    boolean hidden = tc.has("hidden") && tc.get("hidden").asBoolean();
                    results.add(new TestResult(false, jr.stderr(), null, description, hidden));
                }
                return new TestRunSummary(results, 0, testCases.size(), false);
            }
            List<Boolean> flags = judge0ExecutionService.parseBatchStdout(jr.stdout(), testCases.size());
            int passedCount = 0;
            for (int i = 0; i < testCases.size(); i++) {
                JsonNode tc = testCases.get(i);
                String description = tc.has("description") ? tc.get("description").asText() : "Test";
                boolean hidden = tc.has("hidden") && tc.get("hidden").asBoolean();
                boolean passed = i < flags.size() && Boolean.TRUE.equals(flags.get(i));
                if (passed) passedCount++;
                results.add(new TestResult(passed, passed ? null : jr.stdout(), jr.stdout(), description, hidden));
            }
            return new TestRunSummary(results, passedCount, testCases.size(),
                    passedCount == testCases.size());
        } catch (Exception e) {
            for (JsonNode tc : testCases) {
                String description = tc.has("description") ? tc.get("description").asText() : "Test";
                boolean hidden = tc.has("hidden") && tc.get("hidden").asBoolean();
                results.add(new TestResult(false, e.getMessage(), null, description, hidden));
            }
            return new TestRunSummary(results, 0, testCases.size(), false);
        }
    }

    public PlainRunResult runPlain(String code, String input) {
        return runPlain(code, input, "python");
    }

    public PlainRunResult runPlain(String code, String input, String language) {
        if (judge0ExecutionService.shouldUseJudge0(language)) {
            var jr = judge0ExecutionService.runSource(code, language, input);
            return new PlainRunResult(jr.stdout(), jr.stderr(), jr.success());
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("mode", "run");
        payload.put("code", code);
        payload.put("input", input == null ? "" : input);

        JsonNode out = execute(payload);
        if (out == null || out.has("errored")) {
            return new PlainRunResult("", "Execution failed or timed out", false);
        }
        return new PlainRunResult(
                out.path("stdout").asText(""),
                out.path("stderr").asText(""),
                out.path("success").asBoolean(false)
        );
    }

    public PlainRunResult runWithSample(String code, List<Object> args, String callExpr) {
        return runWithSample(code, args, callExpr, "python");
    }

    public PlainRunResult runWithSample(String code, List<Object> args, String callExpr, String language) {
        if (judge0ExecutionService.shouldUseJudge0(language)) {
            try {
                String source = judge0ExecutionService.buildSampleSource(code, language, args, callExpr);
                var jr = judge0ExecutionService.runSource(source, language);
                return new PlainRunResult(jr.stdout(), jr.stderr(), jr.success());
            } catch (Exception e) {
                return new PlainRunResult("", e.getMessage(), false);
            }
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("mode", "run_sample");
        payload.put("code", code);
        payload.set("args", objectMapper.valueToTree(args));
        if (callExpr != null && !callExpr.isBlank()) {
            payload.put("call_expr", callExpr);
        }

        JsonNode out = execute(payload);
        if (out == null || out.has("errored")) {
            String err = out != null && out.has("error") ? out.get("error").asText() : "Execution failed or timed out";
            return new PlainRunResult("", err, false);
        }
        return new PlainRunResult(
                out.path("stdout").asText(""),
                out.path("stderr").asText(""),
                out.path("success").asBoolean(false)
        );
    }

    public JsonNode analyze(String code) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("mode", "analyze");
        payload.put("code", code);
        JsonNode out = execute(payload);
        return out;
    }

    private JsonNode execute(ObjectNode payload) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonBin, runnerScriptPath.toString());
            pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
            pb.redirectErrorStream(false);
            Process process = pb.start();

            process.getOutputStream().write(objectMapper.writeValueAsBytes(payload));
            process.getOutputStream().close();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                ObjectNode err = objectMapper.createObjectNode();
                err.put("errored", true);
                err.put("error", "Time Limit Exceeded (" + timeoutSeconds + "s)");
                return err;
            }

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            if (stdout.isBlank()) {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("errored", true);
                err.put("error", stderr.isBlank() ? "No output produced" : truncate(stderr));
                err.put("stderr", truncate(stderr));
                return err;
            }

            try {
                return objectMapper.readTree(truncate(stdout));
            } catch (Exception parseEx) {
                ObjectNode err = objectMapper.createObjectNode();
                err.put("errored", true);
                err.put("error", "Invalid runner output: " + truncate(stdout));
                if (!stderr.isBlank()) {
                    err.put("stderr", truncate(stderr));
                }
                return err;
            }
        } catch (Exception e) {
            ObjectNode err = objectMapper.createObjectNode();
            err.put("errored", true);
            err.put("error", e.getMessage());
            return err;
        }
    }

    private String truncate(String s) {
        return s.length() > MAX_OUTPUT_CHARS ? s.substring(0, MAX_OUTPUT_CHARS) : s;
    }
}
