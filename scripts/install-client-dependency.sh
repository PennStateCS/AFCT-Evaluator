#!/usr/bin/env bash
#
# The evaluator depends on the AFCT client, which is not on Maven Central. A copy of the
# client jar lives in afct-client/, and Maven only finds it once it has been installed into
# your local repository. Run this once before your first build, and again whenever the
# client version in pom.xml changes.
#
#   ./scripts/install-client-dependency.sh
#
# The version is read from pom.xml rather than typed here, so this cannot drift out of date
# the way a copied command line does.
set -euo pipefail
cd "$(dirname "$0")/.."

version=$(python3 -c "import xml.etree.ElementTree as ET; n='{http://maven.apache.org/POM/4.0.0}'; r=ET.parse('pom.xml').getroot(); print(next(d.find(n+'version').text.strip() for d in r.iter(n+'dependency') if d.find(n+'artifactId').text=='afct-client'))")
jar="afct-client/afct-client-v${version}.jar"

if [ ! -f "$jar" ]; then
  echo "ERROR: pom.xml asks for afct-client ${version}, but ${jar} is not in this repository." >&2
  echo "Copies that are here:" >&2
  ls -1 afct-client/*.jar >&2 || true
  echo "Add the jar for ${version} from the AFCT-Client releases, or point pom.xml at a version that is here." >&2
  exit 1
fi

echo "Installing afct-client ${version} from ${jar}"
mvn -B install:install-file \
  -Dfile="$jar" \
  -DgroupId=edu.rit.cs \
  -DartifactId=afct-client \
  -Dversion="$version" \
  -Dpackaging=jar
