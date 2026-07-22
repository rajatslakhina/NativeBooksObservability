#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PACKAGE_DIR="$PROJECT_DIR/Packages/SharedKMP"
OUTPUT="$PACKAGE_DIR/SharedKit.xcframework"

export JAVA_HOME="$(/usr/libexec/java_home -v 21)"

cd "$PROJECT_DIR"
./gradlew \
  :shared:linkReleaseFrameworkIosArm64 \
  :shared:linkReleaseFrameworkIosSimulatorArm64 \
  --no-daemon \
  --no-watch-fs

if [[ "$OUTPUT" != "$PACKAGE_DIR/SharedKit.xcframework" ]]; then
  echo "Unexpected XCFramework output path" >&2
  exit 1
fi

rm -rf "$OUTPUT"
xcodebuild -create-xcframework \
  -framework "$PROJECT_DIR/shared/build/bin/iosArm64/releaseFramework/SharedKit.framework" \
  -framework "$PROJECT_DIR/shared/build/bin/iosSimulatorArm64/releaseFramework/SharedKit.framework" \
  -output "$OUTPUT"

echo "Built $OUTPUT"
