"""Print per-module surefire totals and every skipped/failed test with its reason."""
import glob
import os
import sys
import xml.etree.ElementTree as ET

roots = sys.argv[1:] or ["."]
total = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
per_module = {}
skipped = []
failed = []
for root in roots:
    pattern = os.path.join(root, "**", "target", "surefire-reports", "TEST-*.xml")
    for path in sorted(glob.glob(pattern, recursive=True)):
        module = path.split(os.sep + "target" + os.sep)[0]
        suite = ET.parse(path).getroot()
        counts = per_module.setdefault(module, dict.fromkeys(total, 0))
        for key in total:
            value = int(float(suite.get(key, "0") or 0))
            counts[key] += value
            total[key] += value
        for case in suite.iter("testcase"):
            name = "%s.%s" % (case.get("classname"), case.get("name"))
            skip = case.find("skipped")
            if skip is not None:
                skipped.append((name, skip.get("message") or (skip.text or "").strip() or "@Ignore (no message)"))
            for tag in ("failure", "error"):
                node = case.find(tag)
                if node is not None:
                    failed.append((name, tag, node.get("message") or ""))

for module, counts in sorted(per_module.items()):
    passed = counts["tests"] - counts["failures"] - counts["errors"] - counts["skipped"]
    print("MODULE %s: tests=%d passed=%d failures=%d errors=%d skipped=%d" % (
        module, counts["tests"], passed, counts["failures"], counts["errors"], counts["skipped"]))
passed = total["tests"] - total["failures"] - total["errors"] - total["skipped"]
print("SUMMARY tests=%d passed=%d failures=%d errors=%d skipped=%d" % (
    total["tests"], passed, total["failures"], total["errors"], total["skipped"]))
for name, reason in skipped:
    print("SKIPPED %s -- %s" % (name, reason))
for name, tag, message in failed:
    print("FAILED(%s) %s -- %s" % (tag, name, message))
