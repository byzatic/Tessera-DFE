#!/usr/bin/env python3
"""Render Surefire/Failsafe JUnit XML as GitHub summary and standalone HTML."""

from __future__ import annotations

import argparse
import html
import os
from pathlib import Path
import xml.etree.ElementTree as ET


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("reports", nargs="*", type=Path)
    return parser.parse_args()


def test_cases(report_paths: list[Path]) -> list[dict[str, str | float]]:
    cases: list[dict[str, str | float]] = []
    for report_path in report_paths:
        if not report_path.is_file():
            continue
        root = ET.parse(report_path).getroot()
        for case in root.iter("testcase"):
            state = "passed"
            detail = ""
            for candidate in ("failure", "error", "skipped"):
                result = case.find(candidate)
                if result is not None:
                    state = candidate
                    detail = (result.get("message") or result.text or "").strip()
                    break
            cases.append(
                {
                    "suite": case.get("classname", "unknown"),
                    "name": case.get("name", "unknown"),
                    "state": state,
                    "time": float(case.get("time", "0") or 0),
                    "detail": detail,
                }
            )
    return cases


def counts(cases: list[dict[str, str | float]]) -> dict[str, int]:
    return {
        state: sum(case["state"] == state for case in cases)
        for state in ("passed", "failure", "error", "skipped")
    }


def write_summary(cases: list[dict[str, str | float]], totals: dict[str, int]) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    status = "PASS" if cases and not totals["failure"] and not totals["error"] else "FAIL"
    lines = [
        "## Test report",
        "",
        "| Result | Total | Passed | Failed | Errors | Skipped | Duration |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
        (
            f"| **{status}** | {len(cases)} | {totals['passed']} | "
            f"{totals['failure']} | {totals['error']} | {totals['skipped']} | "
            f"{sum(float(case['time']) for case in cases):.3f}s |"
        ),
        "",
    ]
    unsuccessful = [case for case in cases if case["state"] in ("failure", "error")]
    if unsuccessful:
        lines.extend(["### Unsuccessful tests", ""])
        for case in unsuccessful[:20]:
            lines.append(f"- `{case['suite']}.{case['name']}` — {case['state']}")
        if len(unsuccessful) > 20:
            lines.append(f"- …and {len(unsuccessful) - 20} more; see the HTML artifact.")
        lines.append("")
    elif not cases:
        lines.extend(["No JUnit XML test results were produced.", ""])

    with open(summary_path, "a", encoding="utf-8") as summary:
        summary.write("\n".join(lines))


def write_html(output: Path, cases: list[dict[str, str | float]], totals: dict[str, int]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    status = "PASS" if cases and not totals["failure"] and not totals["error"] else "FAIL"
    rows = []
    for case in cases:
        detail = html.escape(str(case["detail"]))
        rows.append(
            "<tr>"
            f"<td><span class=\"status {case['state']}\">{case['state']}</span></td>"
            f"<td>{html.escape(str(case['suite']))}</td>"
            f"<td>{html.escape(str(case['name']))}</td>"
            f"<td>{float(case['time']):.3f}s</td>"
            f"<td><pre>{detail}</pre></td>"
            "</tr>"
        )

    empty_row = "<tr><td colspan=\"5\">No JUnit XML test results were produced.</td></tr>"
    document = f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Tessera-DFE test report</title>
  <style>
    body {{ color: #1f2328; font: 14px system-ui, sans-serif; margin: 2rem; }}
    h1 {{ margin-bottom: .25rem; }}
    .summary {{ display: flex; flex-wrap: wrap; gap: .75rem; margin: 1.5rem 0; }}
    .metric {{ border: 1px solid #d0d7de; border-radius: 6px; padding: .75rem 1rem; }}
    table {{ border-collapse: collapse; width: 100%; }}
    th, td {{ border: 1px solid #d0d7de; padding: .5rem; text-align: left; vertical-align: top; }}
    th {{ background: #f6f8fa; }}
    pre {{ margin: 0; max-width: 60rem; overflow-wrap: anywhere; white-space: pre-wrap; }}
    .status {{ font-weight: 600; text-transform: uppercase; }}
    .passed {{ color: #1a7f37; }} .failure, .error {{ color: #cf222e; }} .skipped {{ color: #9a6700; }}
  </style>
</head>
<body>
  <h1>Tessera-DFE test report</h1>
  <p>Overall result: <strong>{status}</strong></p>
  <div class="summary">
    <div class="metric">Total: <strong>{len(cases)}</strong></div>
    <div class="metric">Passed: <strong>{totals['passed']}</strong></div>
    <div class="metric">Failed: <strong>{totals['failure']}</strong></div>
    <div class="metric">Errors: <strong>{totals['error']}</strong></div>
    <div class="metric">Skipped: <strong>{totals['skipped']}</strong></div>
    <div class="metric">Duration: <strong>{sum(float(case['time']) for case in cases):.3f}s</strong></div>
  </div>
  <table>
    <thead><tr><th>Result</th><th>Suite</th><th>Test</th><th>Duration</th><th>Details</th></tr></thead>
    <tbody>{''.join(rows) if rows else empty_row}</tbody>
  </table>
</body>
</html>
"""
    output.write_text(document, encoding="utf-8")


def main() -> None:
    args = parse_args()
    cases = test_cases(args.reports)
    totals = counts(cases)
    write_summary(cases, totals)
    write_html(args.output, cases, totals)


if __name__ == "__main__":
    main()
