#!/usr/bin/env bash
set -uo pipefail

repository="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
retry_delay="${SLC_GRADLE_RETRY_DELAY_SECONDS:-10}"
if [[ ! "$retry_delay" =~ ^[0-9]+$ ]]; then
  echo 'SLC_GRADLE_RETRY_DELAY_SECONDS must be a nonnegative integer.' >&2
  exit 2
fi

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

for attempt in 1 2 3; do
  "$repository/gradlew" "$@" 2>&1 | tee "$log"
  statuses=("${PIPESTATUS[@]}")
  status="${statuses[0]}"
  if (( status == 0 )); then
    exit "${statuses[1]}"
  fi

  # Programming errors need a fix, not repeated builds. Preserve their exit status.
  if grep -Eiq 'Compilation failed|error:|Script compilation errors?' "$log"; then
    exit "$status"
  fi
  # Retry only transient repository/network failures, including failures surfaced
  # while Gradle serializes NFRT's lazily resolved configuration-cache inputs.
  if ! grep -Eiq 'Received status code (408|429|5[0-9]{2})|HTTP (response code: )?(408|429|5[0-9]{2})|Read timed out|Connect timed out|Connection (reset|timed out)|SocketTimeoutException|Remote host terminated the handshake|Temporary failure in name resolution' "$log"; then
    exit "$status"
  fi
  if (( attempt == 3 )); then
    exit "$status"
  fi

  echo "Transient Gradle download failure; retrying ($((attempt + 1))/3) in $((retry_delay * attempt)) seconds." >&2
  sleep "$((retry_delay * attempt))"
done
