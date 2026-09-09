#!/usr/bin/env bash
#
# The evaluator is built against the AFCT client, which is not on Maven Central. This puts
# the right client jar into your local Maven repository so the build can resolve it. Run it
# once, and again whenever the client version in pom.xml changes:
#
#   ./scripts/install-client-dependency.sh
#
# The version comes from pom.xml rather than being typed here, so this cannot drift out of
# date the way a copied command line does. And it is a fixed version, never "the newest
# client": the same evaluator source has to build into the same program every time, which
# matters more here than in most projects, because this one decides marks.
#
# The jar is downloaded from the client's releases. A copy in afct-client/ is used instead
# when there is one, which keeps older versions building and lets you work offline.
set -euo pipefail
cd "$(dirname "$0")/.."

CLIENT_REPO=PennStateCS/AFCT-Client

version=$(python3 -c "import xml.etree.ElementTree as ET; n='{http://maven.apache.org/POM/4.0.0}'; r=ET.parse('pom.xml').getroot(); print(next(d.find(n+'version').text.strip() for d in r.iter(n+'dependency') if d.find(n+'artifactId').text=='afct-client'))")
name="afct-client-v${version}.jar"
vendored="afct-client/${name}"

cleanup() { [ -n "${tmp:-}" ] && rm -rf "$tmp"; }
trap cleanup EXIT

if [ -f "$vendored" ]; then
  jar="$vendored"
  echo "Using the copy already in this repository: ${jar}"
else
  url="https://github.com/${CLIENT_REPO}/releases/download/v${version}/${name}"
  tmp=$(mktemp -d)
  jar="${tmp}/${name}"
  echo "Downloading afct-client ${version} from ${url}"
  if ! curl -fsSL --retry 3 -o "$jar" "$url"; then
    echo "ERROR: could not download ${name}." >&2
    echo "pom.xml asks for afct-client ${version}, so there must be a release tagged v${version}" >&2
    echo "at https://github.com/${CLIENT_REPO}/releases with that jar attached." >&2
    echo "Either release that version of the client, put the jar in afct-client/, or point" >&2
    echo "pom.xml at a version that exists." >&2
    exit 1
  fi
fi

# Printed so the log records exactly which bytes went into the build. Nothing checks this
# against a stored value; it is here so that a surprising result can be traced to a jar.
echo "sha256: $(sha256sum "$jar" | cut -d' ' -f1)"

mvn -B install:install-file \
  -Dfile="$jar" \
  -DgroupId=edu.rit.cs \
  -DartifactId=afct-client \
  -Dversion="$version" \
  -Dpackaging=jar
