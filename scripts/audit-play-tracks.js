#!/usr/bin/env node
/**
 * List Play tracks / versionCodes for a package. Fail if a version sits on phone tracks.
 * Usage (CI): PLAY_SERVICE_ACCOUNT_JSON='{...}' node scripts/audit-play-tracks.js --package com.eazpire.creator.wear --version 21
 */
const PHONE_TRACKS = new Set(['internal', 'alpha', 'beta', 'production', 'rollout']);

function parseArgs() {
  const args = process.argv.slice(2);
  const out = { package: '', version: null, json: false };
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--package') out.package = args[++i];
    else if (args[i] === '--version') out.version = Number(args[++i]);
    else if (args[i] === '--json') out.json = true;
  }
  return out;
}

async function getPublisher() {
  const raw = process.env.PLAY_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error('PLAY_SERVICE_ACCOUNT_JSON missing');
  const creds = JSON.parse(raw);
  const { google } = await import('googleapis');
  const auth = new google.auth.GoogleAuth({
    credentials: creds,
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });
  return google.androidpublisher({ version: 'v3', auth });
}

async function listTrackVersions(publisher, packageName) {
  const edit = await publisher.edits.insert({ packageName });
  const editId = edit.data.id;
  try {
    const res = await publisher.edits.tracks.list({ packageName, editId });
    const tracks = res.data.tracks || [];
    const summary = [];
    for (const t of tracks) {
      const name = t.track || '(unknown)';
      const codes = [];
      for (const rel of t.releases || []) {
        for (const vc of rel.versionCodes || []) codes.push(Number(vc));
      }
      summary.push({ track: name, versionCodes: [...new Set(codes)].sort((a, b) => a - b) });
    }
    return summary;
  } finally {
    try {
      await publisher.edits.delete({ packageName, editId });
    } catch {
      /* ignore */
    }
  }
}

async function main() {
  const { package: packageName, version, json } = parseArgs();
  if (!packageName) {
    console.error('Usage: node scripts/audit-play-tracks.js --package com.eazpire.creator.wear [--version 21]');
    process.exit(1);
  }

  const publisher = await getPublisher();
  const summary = await listTrackVersions(publisher, packageName);

  if (json) {
    console.log(JSON.stringify(summary, null, 2));
    return;
  }

  console.log(`Play tracks for ${packageName}:`);
  for (const row of summary) {
    console.log(`  ${row.track}: ${row.versionCodes.join(', ') || '(none)'}`);
  }

  const onPhone = summary.filter(
    (r) => !r.track.startsWith('wear:') && PHONE_TRACKS.has(r.track) && r.versionCodes.length
  );
  const bad =
    version != null
      ? onPhone.filter((r) => r.versionCodes.includes(version))
      : onPhone.filter((r) => r.versionCodes.some((c) => c > 1));

  if (bad.length) {
    console.error('\n::error::Wear-only bundles found on PHONE tracks (causes android.hardware.type.watch error):');
    for (const r of bad) {
      console.error(`  track "${r.track}" has versionCodes: ${r.versionCodes.join(', ')}`);
    }
    console.error(
      '\nFix in Play Console: Test and release → Interner Test (phone, NOT "Wear OS") → open release → remove Wear bundle(s).'
    );
    console.error('Keep bundles only under: Wear OS → Interner Test (Wear OS).');
    process.exit(1);
  }

  if (version != null) {
    const wearRows = summary.filter((r) => r.track.startsWith('wear:') && r.versionCodes.includes(version));
    if (!wearRows.length) {
      console.warn(`::warning::versionCode ${version} not found on any wear:* track yet (upload delay?).`);
    } else {
      console.log(`OK: versionCode ${version} on wear track(s): ${wearRows.map((r) => r.track).join(', ')}`);
    }
  }
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
