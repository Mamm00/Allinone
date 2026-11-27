#!/usr/bin/env python3
"""Emit one bounded, multiline Actions annotation, not hundreds of truncated stack frames."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

log = Path(sys.argv[1])
text = log.read_text(errors="replace") if log.exists() else "Test log not available"
messages = []
for file in Path("app/build").rglob("*.xml"):
    try:
        root = ET.parse(file).getroot()
        for case in root.iter("testcase"):
            for failure in list(case.findall("failure")) + list(case.findall("error")):
                detail = "\n".join((failure.text or failure.get("message", "")).splitlines()[:18])
                messages.append(f'{case.get("classname", "")}.{case.get("name", "")}:\n{detail}')
    except (ET.ParseError, OSError):
        continue
lines = text.splitlines()
for index, line in enumerate(lines):
    if re.search(r"^e: |^error: |^.*(?:Test|test) > .*FAILED|^.* > .*FAILED", line):
        messages.append("\n".join(lines[index:index+12]))
if not messages:
    messages = [line for line in lines if re.search(r"Caused by:|FAILURES|AssertionError|Exception:|Execution failed|What went wrong|failed", line)][:20]
summary = "\n\n".join(messages[:8])[:14000] or "No test assertion details found. Inspect the uploaded test reports."
summary = summary.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print(f"::error::Test failure details: {summary}")
