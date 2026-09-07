import { neon, type NeonQueryFunction } from '@neondatabase/serverless';

let ensured = false;

export function getSql(): NeonQueryFunction<false, false> | null {
  const url = process.env.DATABASE_URL;
  if (!url) return null;
  return neon(url);
}

/**
 * Creates the shared_items table on first use of a cold edge instance.
 * IF NOT EXISTS keeps this safe to run on every cold start.
 */
export async function ensureSharedTable(sql: NeonQueryFunction<false, false>) {
  if (ensured) return;
  await sql`
    CREATE TABLE IF NOT EXISTS shared_items (
      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
      created_at timestamptz NOT NULL DEFAULT now(),
      updated_at timestamptz NOT NULL DEFAULT now(),
      type text NOT NULL CHECK (type IN ('template','glyph')),
      status text NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','approved','rejected')),
      title text NOT NULL,
      description text NOT NULL DEFAULT '',
      handle text NOT NULL,
      email text NOT NULL DEFAULT '',
      github text NOT NULL DEFAULT '',
      payload jsonb NOT NULL,
      analysis jsonb NOT NULL DEFAULT '{}'::jsonb,
      content_hash text NOT NULL DEFAULT '',
      summary text NOT NULL DEFAULT '',
      capabilities text[] NOT NULL DEFAULT '{}',
      moderated_by text,
      moderated_at timestamptz,
      moderation_reason text NOT NULL DEFAULT '',
      ip_hash text NOT NULL DEFAULT '',
      downloads integer NOT NULL DEFAULT 0
    )
  `;
  await sql`
    CREATE INDEX IF NOT EXISTS shared_items_list_idx
      ON shared_items (status, type, created_at DESC)
  `;
  ensured = true;
}

export async function sha256Hex(input: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(input));
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

/** Bearer auth — accepts ADMIN_TOKEN, falls back to CRASH_ADMIN_TOKEN. */
export function isAdmin(req: Request): boolean {
  const h = req.headers as any;
  const auth =
    typeof h.get === 'function' ? h.get('authorization') ?? '' : h['authorization'] ?? '';
  const token = auth.startsWith('Bearer ') ? auth.slice(7) : '';
  if (!token) return false;
  const allowed = [process.env.ADMIN_TOKEN, process.env.CRASH_ADMIN_TOKEN].filter(Boolean);
  return allowed.includes(token);
}
