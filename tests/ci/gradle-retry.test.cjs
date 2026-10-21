const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '../..');
const build = path.join(root, 'build');
fs.mkdirSync(build, { recursive: true });
const fixture = fs.mkdtempSync(path.join(build, 'ci-retry-test-'));
try {
  fs.mkdirSync(path.join(fixture, 'scripts'));
  fs.copyFileSync(path.join(root, 'scripts/gradle-ci.sh'), path.join(fixture, 'scripts/gradle-ci.sh'));
  fs.writeFileSync(path.join(fixture, 'gradlew'), `#!/usr/bin/env bash
count=0
[[ ! -f count ]] || read -r count < count
echo "$((++count))" > count
printf '%s\\n' "$@" > arguments
if (( count <= FAIL_COUNT )); then
  echo "$FAIL_MESSAGE" >&2
  exit 7
fi
echo 'BUILD SUCCESSFUL'
`);
  fs.chmodSync(path.join(fixture, 'gradlew'), 0o755);

  const cases = [
    ['success', 0, '', 0, 1],
    ['temporary gateway failure', 1, 'Received status code 502 from server: Bad Gateway', 0, 2],
    ['repository unavailable', 2, 'Received status code 503 from server', 0, 3],
    ['timeout', 1, 'java.net.SocketTimeoutException: Read timed out', 0, 2],
    ['persistent gateway failure', 10, 'Received status code 502 from server', 7, 3],
    ['compiler error', 10, 'Compilation failed; error: missing method', 7, 1],
    ['compiler error after download failure', 10, 'Received status code 502; Compilation failed', 7, 1],
    ['missing dependency', 10, 'Could not find dependency; Received status code 404', 7, 1],
    ['unknown failure', 10, 'Task failed for an unknown reason', 7, 1],
  ];
  for (const [name, failures, message, status, attempts] of cases) {
    fs.writeFileSync(path.join(fixture, 'count'), '0\n');
    const result = spawnSync(process.env.SLC_TEST_BASH || 'bash', ['scripts/gradle-ci.sh', ':1.18.2-forge:build', '-Pbuild_targets=1.18.2-forge'], {
      cwd: fixture, encoding: 'utf8',
      env: { ...process.env, SLC_GRADLE_RETRY_DELAY_SECONDS: '0', FAIL_COUNT: String(failures), FAIL_MESSAGE: message },
    });
    assert.ifError(result.error);
    assert.equal(result.status, status, `${name}: ${result.stdout}\n${result.stderr}`);
    assert.equal(Number(fs.readFileSync(path.join(fixture, 'count'), 'utf8')), attempts, name);
    assert.equal(fs.readFileSync(path.join(fixture, 'arguments'), 'utf8'), ':1.18.2-forge:build\n-Pbuild_targets=1.18.2-forge\n');
  }
  console.log(`Gradle CI retry tests passed (${cases.length} scenarios)`);
} finally {
  // Only remove the temporary child created above, within this repository's build directory.
  assert.equal(path.dirname(path.resolve(fixture)), build);
  assert.ok(path.basename(fixture).startsWith('ci-retry-test-'));
  fs.rmSync(fixture, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 });
}
