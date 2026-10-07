"""
CodeSim sandbox runner — invoked by the Spring Boot backend via subprocess, one
process per operation (single test case / plain run / static analysis).

Reads a single JSON payload from stdin, writes a single JSON result to stdout.
Ported from the original Flask app's code_executor.py + ai_engine.py CodeAnalyzer,
adapted to a structured {"args": [...], "expected_output": ...} test case shape
instead of an evaluable function_call string.
"""
import ast
import json
import re
import sys


def compare_outputs(actual, expected):
    try:
        if actual == expected:
            return True
        if isinstance(expected, (int, float)) or isinstance(actual, (int, float)):
            return abs(float(actual) - float(expected)) < 1e-6
        if str(actual).strip() == str(expected).strip():
            return True
        if isinstance(actual, list) and isinstance(expected, list):
            return sorted(str(x) for x in actual) == sorted(str(x) for x in expected)
        return False
    except Exception:
        return str(actual) == str(expected)


def run_single_test(code, args, expected_output, call_expr=None):
    ns = {}
    try:
        exec(code, ns)
    except Exception as e:
        return {"passed": False, "error": f"Syntax/definition error: {e}", "actual": None}

    if call_expr:
        try:
            result = eval(call_expr, ns)
            if isinstance(result, tuple):
                result = list(result)
            passed = compare_outputs(result, expected_output)
            return {"passed": passed, "error": None, "actual": result}
        except Exception as e:
            return {"passed": False, "error": str(e), "actual": None}

    func = ns.get("solution")
    if not callable(func):
        return {"passed": False, "error": "No `solution` function defined", "actual": None}

    try:
        result = func(*args)
        if isinstance(result, tuple):
            result = list(result)
        passed = compare_outputs(result, expected_output)
        return {"passed": passed, "error": None, "actual": result}
    except Exception as e:
        return {"passed": False, "error": str(e), "actual": None}


def run_with_sample(code, args, call_expr=None):
    """Execute solution with sample test args and return formatted output for the Run button."""
    ns = {}
    try:
        exec(code, ns)
    except Exception as e:
        return {"stdout": "", "stderr": f"Syntax/definition error: {e}", "success": False}

    try:
        if call_expr:
            result = eval(call_expr, ns)
        else:
            func = ns.get("solution")
            if not callable(func):
                return {"stdout": "", "stderr": "No `solution` function defined", "success": False}
            result = func(*args)
        if isinstance(result, tuple):
            result = list(result)
        return {"stdout": f"Result: {result!r}", "stderr": "", "success": True}
    except Exception as e:
        return {"stdout": "", "stderr": str(e), "success": False}


def run_plain(code, custom_input):
    import io
    import contextlib

    old_stdin = sys.stdin
    sys.stdin = io.StringIO(custom_input)
    stdout_buf = io.StringIO()
    stderr_buf = io.StringIO()
    success = True
    try:
        with contextlib.redirect_stdout(stdout_buf), contextlib.redirect_stderr(stderr_buf):
            exec(code, {"__name__": "__main__"})
    except SystemExit:
        pass
    except Exception as e:
        stderr_buf.write(str(e))
        success = False
    finally:
        sys.stdin = old_stdin

    return {
        "stdout": stdout_buf.getvalue()[:5000],
        "stderr": stderr_buf.getvalue()[:5000],
        "success": success,
    }


class CodeVisitor(ast.NodeVisitor):
    def __init__(self):
        self.num_functions = 0
        self.num_classes = 0
        self.cyclomatic_complexity = 1
        self.has_recursion = False
        self.used_collections = set()
        self.max_loop_depth = 0
        self.max_nesting_depth = 0
        self.warnings = []
        self._current_depth = 0
        self._loop_depth = 0
        self._current_function = None

    def visit_FunctionDef(self, node):
        self.num_functions += 1
        prev_fn = self._current_function
        self._current_function = node.name
        self.generic_visit(node)
        self._current_function = prev_fn

    def visit_AsyncFunctionDef(self, node):
        self.visit_FunctionDef(node)

    def visit_ClassDef(self, node):
        self.num_classes += 1
        self.generic_visit(node)

    def visit_If(self, node):
        self.cyclomatic_complexity += 1
        self._current_depth += 1
        self.max_nesting_depth = max(self.max_nesting_depth, self._current_depth)
        self.generic_visit(node)
        self._current_depth -= 1

    def visit_For(self, node):
        self.cyclomatic_complexity += 1
        self._loop_depth += 1
        self._current_depth += 1
        self.max_loop_depth = max(self.max_loop_depth, self._loop_depth)
        self.max_nesting_depth = max(self.max_nesting_depth, self._current_depth)
        self.generic_visit(node)
        self._loop_depth -= 1
        self._current_depth -= 1

    def visit_While(self, node):
        self.visit_For(node)

    def visit_ExceptHandler(self, node):
        self.cyclomatic_complexity += 1
        self.generic_visit(node)

    def visit_Call(self, node):
        if self._current_function:
            if isinstance(node.func, ast.Name) and node.func.id == self._current_function:
                self.has_recursion = True
            elif isinstance(node.func, ast.Attribute) and node.func.attr == self._current_function:
                self.has_recursion = True

        if isinstance(node.func, ast.Name):
            name = node.func.id
            if name in ("dict", "list", "set", "tuple", "defaultdict", "Counter", "deque", "heapq", "OrderedDict"):
                self.used_collections.add(name)

        self.generic_visit(node)

    def visit_Import(self, node):
        for alias in node.names:
            if alias.name in ("collections", "heapq", "bisect", "sortedcontainers"):
                self.used_collections.add(alias.name)
        self.generic_visit(node)


def analyze_code(code):
    result = {
        "lines_of_code": 0, "num_functions": 0, "num_classes": 0,
        "cyclomatic_complexity": 1, "has_recursion": False,
        "uses_builtin_ds": [], "nested_loops": 0, "max_depth": 0,
        "syntax_valid": True, "syntax_error": "", "warnings": [],
        "code_quality_score": 0.0,
    }

    if not code or not code.strip():
        result["syntax_error"] = "Empty code"
        result["syntax_valid"] = False
        return result

    result["lines_of_code"] = sum(1 for l in code.split("\n") if l.strip())

    try:
        tree = ast.parse(code)
    except SyntaxError as e:
        result["syntax_valid"] = False
        result["syntax_error"] = str(e)
        return result

    visitor = CodeVisitor()
    visitor.visit(tree)

    result.update({
        "num_functions": visitor.num_functions,
        "num_classes": visitor.num_classes,
        "cyclomatic_complexity": visitor.cyclomatic_complexity,
        "has_recursion": visitor.has_recursion,
        "uses_builtin_ds": list(visitor.used_collections),
        "nested_loops": visitor.max_loop_depth,
        "max_depth": visitor.max_nesting_depth,
        "warnings": visitor.warnings,
    })

    score = 10.0
    cc = result["cyclomatic_complexity"]
    if cc > 10:
        score -= 2.0
    elif cc > 5:
        score -= 1.0
    if result["nested_loops"] > 2:
        score -= 1.5
    elif result["nested_loops"] > 1:
        score -= 0.5
    if result["lines_of_code"] > 100:
        score -= 1.0
    score -= len(result["warnings"]) * 0.5
    result["code_quality_score"] = max(0.0, min(10.0, round(score, 2)))

    return result


def estimate_complexity(code):
    result = {"time": "O(?)", "space": "O(1)", "notes": ""}

    nested_loops = len(re.findall(r"\bfor\b|\bwhile\b", code))
    has_recursion = bool(re.search(r"def\s+(\w+)[^:]+:.*\1\s*\(", code, re.DOTALL))
    has_sorting = bool(re.search(r"\.sort\b|sorted\(", code))
    has_binary_search = bool(re.search(r"bisect|binary.search", code, re.I))

    if nested_loops >= 3:
        result["time"] = "O(n³) or worse"
    elif nested_loops == 2:
        result["time"] = "O(n²)"
    elif nested_loops == 1:
        if has_sorting:
            result["time"] = "O(n log n)"
        elif has_binary_search:
            result["time"] = "O(log n)"
        else:
            result["time"] = "O(n)"
    elif has_recursion:
        result["time"] = "O(2ⁿ) possibly — check recurrence"
    elif has_sorting:
        result["time"] = "O(n log n)"
    else:
        result["time"] = "O(n) or O(1)"

    has_list_growth = bool(re.search(r"\.append|\.extend|\[\]|list\(", code))
    has_dict_growth = bool(re.search(r"\{\}|dict\(|defaultdict", code))
    if has_list_growth or has_dict_growth:
        result["space"] = "O(n)"

    return result


def main():
    payload = json.loads(sys.stdin.read())
    mode = payload.get("mode")

    if mode == "test_single":
        result = run_single_test(payload["code"], payload.get("args", []), payload.get("expected_output"),
                                  payload.get("call_expr"))
    elif mode == "test_all":
        cases = payload.get("test_cases") or []
        rows = []
        for tc in cases:
            row = run_single_test(
                payload["code"],
                tc.get("args", []),
                tc.get("expected_output"),
                tc.get("call_expr"),
            )
            rows.append(row)
        result = {"results": rows}
    elif mode == "run":
        result = run_plain(payload["code"], payload.get("input", ""))
    elif mode == "run_sample":
        result = run_with_sample(
            payload["code"],
            payload.get("args", []),
            payload.get("call_expr"),
        )
    elif mode == "analyze":
        result = {
            "analysis": analyze_code(payload["code"]),
            "complexity": estimate_complexity(payload["code"]),
        }
    else:
        result = {"error": f"Unknown mode: {mode}"}

    print(json.dumps(result))


if __name__ == "__main__":
    main()
