const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '../..');
const workflow = fs.readFileSync(path.join(root, '.github/workflows/release.yml'), 'utf8');
const match = workflow.match(/awk -v version="\$MOD_VERSION" '([\s\S]*?)' CHANGELOG\.md/);
assert.ok(match, 'Release workflow must extract notes from CHANGELOG.md');

function extract(version, input) {
  const result = spawnSync(process.env.SLC_TEST_BASH || 'bash', [
    '-c', 'awk -v version="$1" "$2"', 'release-notes-test', version, match[1],
  ], { input, encoding: 'utf8' });
  assert.ifError(result.error);
  assert.equal(result.status, 0, result.stderr);
  return result.stdout.trim();
}

assert.equal(extract('4.3.0', `# Changelog
## [Unreleased]
- Future change.
## [4.3.0] - 2026-11-14
### Added
- New option.
### Fixed
- Rebuild fix.
## [4.2.0] - 2026-11-07
- Older change.
`), '### Added\n- New option.\n### Fixed\n- Rebuild fix.');
assert.equal(extract('4.2.0', '# [4.2.0]\n\n- Legacy notes.\n# [4.1.0]\n- Older notes.\n'), '- Legacy notes.');
assert.equal(extract('4.3.0', '## [4.3.0-beta.1]\n- Beta notes.\n## [4.3.0+test]\n- Build notes.\n'), '');
assert.equal(extract('4.3.0-beta.1+test', '## [4.3.0-beta.1+test] - 2026-11-14\n- Prerelease notes.\n## [4.2.0]\n- Older notes.\n'), '- Prerelease notes.');

const changelog = fs.readFileSync(path.join(root, 'CHANGELOG.md'), 'utf8');
const properties = fs.readFileSync(path.join(root, 'stonecutter.properties.toml'), 'utf8');
const version = properties.match(/^mod\.version = "([^"]+)"$/m)[1];
const notes = extract(version, changelog);
assert.match(notes, /^### (Added|Changed|Deprecated|Removed|Fixed|Security)\n/, 'Current release must include categorized notes');
assert.match(notes, /^- /m, 'Current release must document its changes');
assert.doesNotMatch(notes, /^##? \[/m, 'Current release must exclude other release headings');
assert.doesNotMatch(notes, /^\[[^\]]+\]: /m, 'Current release must exclude reference links');
console.log('Release note extraction tests passed (5 scenarios)');
