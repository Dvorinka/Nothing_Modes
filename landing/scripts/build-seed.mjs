/**
 * Build step: bundle the demo templates and glyph museum presets into
 * `landing/seed.json` so the deployed seed endpoint can read it at runtime.
 *
 * Run with: node landing/scripts/build-seed.mjs
 */
import { readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(path.join(__dirname, '..', '..'));
const TEMPLATES_DIR = path.join(REPO_ROOT, 'templates');
const GLYPH_DIR = path.join(REPO_ROOT, 'nothing-integrations', 'src', 'main', 'assets', 'glyph_presets');
const OUT = path.join(REPO_ROOT, 'landing', 'seed.json');

const items = [];

// demo templates
const indexRaw = await readFile(path.join(TEMPLATES_DIR, 'index.json'), 'utf8');
const index = JSON.parse(indexRaw);
for (const t of index.templates) {
  const payload = JSON.parse(await readFile(path.join(TEMPLATES_DIR, t.file), 'utf8'));
  items.push({
    type: 'template',
    title: t.name,
    description: t.description,
    payload,
    file: t.file,
  });
}

// glyph museum presets
const glyphFiles = (await readdir(GLYPH_DIR)).filter((f) => f.endsWith('.json'));
for (const f of glyphFiles) {
  const name = f.replace(/\.json$/, '').replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  const payload = JSON.parse(await readFile(path.join(GLYPH_DIR, f), 'utf8'));
  items.push({
    type: 'glyph',
    title: name,
    description: '',
    payload,
    file: f,
  });
}

await writeFile(OUT, JSON.stringify({ items }, null, 2));
console.log(`seed.json: ${items.length} items (${items.filter((i) => i.type === 'template').length} templates, ${items.filter((i) => i.type === 'glyph').length} glyphs)`);
