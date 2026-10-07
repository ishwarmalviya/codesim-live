import type { EditorLang } from '../components/CodeEditor';
import type { QuestionDto } from '../types';

export function starterForLang(q: QuestionDto | undefined | null, lang: EditorLang): string {
  if (!q) return '';
  const byLang = q.starterCodeByLang as Record<string, string> | undefined;
  if (byLang && byLang[lang]) return byLang[lang];
  if (lang === 'python') return q.starterCode;
  if (lang === 'javascript') return 'function solution(...args) {\n  // Write your solution here\n}\n';
  if (lang === 'java') {
    return 'class Solution {\n    public Object solution(int[] nums, int target) {\n        // Write your solution here (adjust args/return type to the problem)\n        return null;\n    }\n}\n';
  }
  if (lang === 'cpp') {
    return '#include <bits/stdc++.h>\nusing namespace std;\n\nvector<int> solution(vector<int> nums, int target) {\n    // Write your solution here (adjust args/return type to the problem)\n    return {};\n}\n';
  }
  if (lang === 'c') {
    return '#include <bits/stdc++.h>\nusing namespace std;\n\nvector<int> solution(vector<int> nums, int target) {\n    // C is run through Judge0 as C++ so vectors work. Adjust the signature to the problem.\n    return {};\n}\n';
  }
  return q.starterCode;
}
