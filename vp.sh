#!/usr/bin/env bash
set -euo pipefail

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null
java -jar ./target/scala-2.12/odyssey-assembly-0.1.5.jar -t VerifiablePresentation "$@"
popd > /dev/null
