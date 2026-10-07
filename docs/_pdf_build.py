# -*- coding: utf-8 -*-
"""Build a printable PDF of the full CodeSim mentor-teaching series."""
from pathlib import Path
from fpdf import FPDF

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "CodeSim_Complete_Mentor_Guide.pdf"
FONT = Path(r"C:\Windows\Fonts\arial.ttf")
FONTB = Path(r"C:\Windows\Fonts\arialbd.ttf")
FONTI = Path(r"C:\Windows\Fonts\ariali.ttf")


class GuidePDF(FPDF):
    def header(self):
        if self.page_no() == 1:
            return
        self.set_font("Body", "I", 8)
        self.set_text_color(90, 90, 90)
        self.set_x(self.l_margin)
        self.cell(0, 8, "CodeSim Complete Mentor Teaching Guide | Modules 1-15 + Cheat Sheet + Mock Viva", align="L")
        self.ln(4)
        self.set_draw_color(180, 180, 180)
        self.line(self.l_margin, 14, 210 - self.r_margin, 14)
        self.ln(6)
        self.set_text_color(0, 0, 0)
        self.set_x(self.l_margin)

    def footer(self):
        self.set_y(-12)
        self.set_font("Body", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 8, f"Page {self.page_no()}/{{nb}}", align="C")


def add_heading(pdf, text, level):
    pdf.set_x(pdf.l_margin)
    pdf.set_text_color(20, 40, 80)
    sizes = {1: (16, 8), 2: (13, 7), 3: (11, 6), 4: (10, 5)}
    size, h = sizes.get(level, (10, 5))
    pdf.ln(3 if level <= 2 else 1)
    pdf.set_font("Body", "B", size)
    pdf.multi_cell(0, h, text)
    pdf.set_x(pdf.l_margin)
    pdf.set_text_color(0, 0, 0)


def add_body(pdf, text):
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Body", "", 9.5)
    pdf.set_text_color(20, 20, 20)
    pdf.multi_cell(0, 5, text)
    pdf.set_x(pdf.l_margin)
    pdf.ln(0.8)


def add_mono(pdf, text):
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Body", "", 8)
    pdf.set_fill_color(245, 245, 248)
    pdf.set_text_color(30, 30, 30)
    pdf.multi_cell(0, 4.2, text, fill=True)
    pdf.set_x(pdf.l_margin)
    pdf.ln(1)
    pdf.set_text_color(0, 0, 0)


def clean(s: str) -> str:
    s = s.replace("**", "").replace("__", "").replace("`", "")
    for a, b in {
        "•": "-",
        "—": "-",
        "–": "-",
        "→": "->",
        "←": "<-",
        "×": "x",
        "’": "'",
        "‘": "'",
        "“": '"',
        "”": '"',
    }.items():
        s = s.replace(a, b)
    return s


def render_markdown(pdf, md: str):
    for raw in md.splitlines():
        line = clean(raw.rstrip())
        if not line.strip():
            pdf.ln(1.2)
            continue
        stripped = line.replace("|", "").replace(" ", "").replace("-", "")
        if not stripped and "---" in raw:
            continue
        if line.startswith("# "):
            add_heading(pdf, line[2:].strip(), 1)
        elif line.startswith("## "):
            add_heading(pdf, line[3:].strip(), 2)
        elif line.startswith("### "):
            add_heading(pdf, line[4:].strip(), 3)
        elif line.startswith("#### "):
            add_heading(pdf, line[5:].strip(), 4)
        elif line.startswith("```"):
            continue
        elif line.startswith("|"):
            add_body(pdf, line.replace("|", " | ").strip(" |"))
        elif line.startswith("- ") or line.startswith("* "):
            add_body(pdf, "- " + line[2:])
        elif len(line) >= 3 and line[0].isdigit() and line[1:3] == ". ":
            add_body(pdf, line)
        else:
            add_body(pdf, line)


def main():
    text = (ROOT / "CodeSim_Complete_Mentor_Guide.md").read_text(encoding="utf-8")
    pdf = GuidePDF(format="A4")
    pdf.set_margins(15, 18, 15)
    pdf.alias_nb_pages()
    pdf.set_auto_page_break(auto=True, margin=16)
    pdf.add_font("Body", "", str(FONT))
    pdf.add_font("Body", "B", str(FONTB))
    pdf.add_font("Body", "I", str(FONTI))
    pdf.add_page()
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Body", "B", 22)
    pdf.ln(16)
    pdf.multi_cell(0, 10, "CodeSim")
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Body", "B", 14)
    pdf.multi_cell(0, 8, "Complete Mentor Teaching Guide")
    pdf.set_x(pdf.l_margin)
    pdf.set_font("Body", "", 11)
    pdf.ln(4)
    pdf.multi_cell(
        0,
        6,
        "The AI-Driven Adaptive Coding Interview Simulator & Dynamic Review Engine\n"
        "Modules 1 through 15, plus one-page cheat sheet and mock viva Q&A.\n"
        "Compiled from the project source and the full teaching series.\n"
        "Do not paste API keys or production secrets into this document.",
    )
    pdf.set_x(pdf.l_margin)
    pdf.ln(6)
    pdf.set_font("Body", "I", 10)
    pdf.multi_cell(0, 5, "Print on A4. Use this to explain the project as if you designed it.")
    pdf.set_x(pdf.l_margin)
    render_markdown(pdf, text)
    pdf.output(str(OUT))
    print(f"Wrote {OUT} ({OUT.stat().st_size} bytes, {pdf.page_no()} pages)")


if __name__ == "__main__":
    main()
