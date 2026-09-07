import { ensureSharedTable, getSql, sha256Hex } from './db';
import { analyzeGlyph, analyzeTemplate } from './analyze';

export interface SeedItem {
  type: 'template' | 'glyph';
  title: string;
  description: string;
  payload: unknown;
}

export async function seedDemos(sql: NonNullable<ReturnType<typeof getSql>>, items: SeedItem[]) {
  for (const item of items) {
    const analysis =
      item.type === 'template' ? analyzeTemplate(item.payload) : analyzeGlyph(item.payload);
    if (analysis.verdict === 'reject') {
      console.warn('seed rejected:', item.title, analysis.findings);
      continue;
    }
    const contentHash = await sha256Hex(JSON.stringify(analysis.sanitized));
    const exists = await sql`
      SELECT 1 FROM shared_items WHERE content_hash = ${contentHash} LIMIT 1
    `;
    if (exists.length > 0) continue;
    await sql`
      INSERT INTO shared_items
        (type, status, title, description, handle, email, github,
         payload, analysis, content_hash, summary, capabilities,
         moderated_by, moderated_at, downloads)
      VALUES (
        ${item.type}, 'approved', ${item.title}, ${item.description},
        'nothing-modes', '', '', ${JSON.stringify(analysis.sanitized)},
        ${JSON.stringify(analysis)}, ${contentHash}, ${analysis.summary},
        ${analysis.capabilities}, 'seed', now(), 0
      )
    `;
  }
}
