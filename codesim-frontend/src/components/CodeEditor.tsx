import CodeMirror from '@uiw/react-codemirror';
import { python } from '@codemirror/lang-python';
import { java } from '@codemirror/lang-java';
import { cpp } from '@codemirror/lang-cpp';
import { javascript } from '@codemirror/lang-javascript';
import { oneDark } from '@codemirror/theme-one-dark';
import { useThemeStore } from '../store/theme';

export type EditorLang = 'python' | 'javascript' | 'java' | 'cpp' | 'c';

function langExtension(lang: EditorLang) {
  switch (lang) {
    case 'java': return java();
    case 'cpp':
    case 'c': return cpp();
    case 'javascript': return javascript();
    default: return python();
  }
}

interface CodeEditorProps {
  value: string;
  onChange?: (value: string) => void;
  language?: EditorLang;
  height?: string;
  readOnly?: boolean;
}

export function CodeEditor({
  value,
  onChange,
  language = 'python',
  height = '55vh',
  readOnly = false,
}: CodeEditorProps) {
  const theme = useThemeStore((s) => s.theme);
  return (
    <CodeMirror
      value={value}
      height={height}
      theme={theme === 'dark' ? oneDark : 'light'}
      extensions={[langExtension(language)]}
      onChange={onChange ?? (() => {})}
      readOnly={readOnly}
      editable={!readOnly}
    />
  );
}
