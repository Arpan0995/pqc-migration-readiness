#!/usr/bin/env bash
#
# Run the PQC readiness auditor on a Java source tree.
#
#   ./scan.sh                       scan the bundled jjwt case study
#   ./scan.sh path/to/project       scan your own code
#   ./scan.sh path/to/project name  scan and label the report
#
# Resolves the auditor jar in this order: a local build, then the released jar
# from Maven Central. Reports are written to ./audit-out (git-ignored).
set -euo pipefail
cd "$(dirname "$0")"

TARGET="${1:-case-studies/jjwt/repo}"
NAME="${2:-$(basename "$TARGET")}"
VERSION="1.4.0"
JAR="auditor/target/pqc-readiness-auditor-${VERSION}-all.jar"

if [ "$TARGET" = "case-studies/jjwt/repo" ] && [ ! -e "$TARGET/pom.xml" ]; then
  echo "Fetching the jjwt case study (git submodule)..."
  git submodule update --init case-studies/jjwt/repo
fi

if [ ! -f "$JAR" ] && command -v mvn >/dev/null 2>&1; then
  echo "Building the auditor (first run only, ~1 min)..."
  mvn -q -pl auditor -am package -DskipTests -Dgpg.skip=true
fi
if [ ! -f "$JAR" ]; then
  echo "Downloading the released auditor jar from Maven Central..."
  mkdir -p auditor/target
  curl -fsSL -o "$JAR" \
    "https://repo1.maven.org/maven2/io/github/arpan0995/pqc-readiness-auditor/${VERSION}/pqc-readiness-auditor-${VERSION}-all.jar"
fi

echo "Scanning ${TARGET} ..."
java -jar "$JAR" "$TARGET" --out audit-out --name "$NAME"
echo
echo "===================== migration plan ====================="
sed -n '/^## Migration plan/,/^## Module ranking/p' audit-out/readiness-report.md | sed '$d'
echo "Full report: audit-out/readiness-report.md (plus .json and .sarif)"
