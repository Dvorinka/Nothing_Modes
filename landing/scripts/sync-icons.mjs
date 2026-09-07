import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const ktPath = resolve(__dirname, '../../ui/src/main/kotlin/com/tdvorak/nothingmodes/ui/screens/IconColorPickerSheet.kt');
const outPath = resolve(__dirname, '../icons.json');

const kt = readFileSync(ktPath, 'utf8');
const entries = [];
const seen = new Set();

for (const m of kt.matchAll(/IconEntry\("([^"]+)",\s*Icons(?:\.AutoMirrored)?(?:\.Outlined)?\.([A-Za-z0-9_]+)\s*,/g)) {
  const name = m[1];
  let vector = m[2];
  if (seen.has(name)) continue;
  seen.add(name);

  // Convert Material 3 ImageVector class names to Material Symbols ligature names.
  const symbol = vector
    .replace(/([A-Z0-9])/g, '_$1')
    .toLowerCase()
    .replace(/^_/, '');

  entries.push({ name, symbol });
}

const data = {
  // Single source of truth: this file is generated from IconColorPickerSheet.kt.
  // Run: node landing/scripts/sync-icons.mjs
  generated_from: 'ui/src/main/kotlin/com/tdvorak/nothingmodes/ui/screens/IconColorPickerSheet.kt',
  fallback: 'star',
  icons: entries,
};

writeFileSync(outPath, JSON.stringify(data, null, 2) + '\n');
console.log(`Wrote ${entries.length} icons to ${outPath}`);
