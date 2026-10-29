import { ensureFeedbackTable, getSql, isAdmin } from './lib/db';
import { envCheck } from './lib/env';
import { esc, nothingShell } from './lib/email';

export const config = { runtime: 'edge' };

const MAX_BODY = 24 * 1024;
const MAX_MESSAGE = 4000;
const MAX_CONTACT = 200;
const EMAIL_DEDUP_WINDOW = '1 hour';
const CATEGORIES = new Set(['bug', 'idea', 'other']);

interface Feedback {
  app?: string;
  category?: string;
  message?: string;
  contact?: string;
  app_version?: string;
  version_code?: number;
  flavor?: string;
  device?: string;
  android_release?: string;
  sdk_int?: number;
  client_time?: number;
}

/**
 * In-app feedback channel.
 *
 * POST — user-initiated (typing and tapping send is the consent); stores the
 * message and emails it to FEEDBACK_NOTIFY_EMAIL (falls back to
 * CRASH_NOTIFY_EMAIL). Identical message+device within an hour still stores
 * but skips the email, like /api/crash.
 *
 * GET — admin list for the dashboard. Authorization: Bearer <ADMIN_TOKEN>
 * (CRASH_ADMIN_TOKEN also accepted).
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method === 'GET') return list(req);
  if (req.method !== 'POST') return new Response('method not allowed', { status: 405 });

  const env = envCheck();
  if (!env.ok) {
    return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });
  }

  const body = await req.text();
  if (body.length > MAX_BODY) return new Response('payload too large', { status: 413 });

  let report: Feedback;
  try {
    report = JSON.parse(body);
  } catch {
    return new Response('invalid json', { status: 400 });
  }
  const message = String(report?.message ?? '').trim();
  if (report?.app !== 'nothing-modes' || !message) {
    return new Response('invalid report', { status: 400 });
  }

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureFeedbackTable(sql);

  const category = CATEGORIES.has(String(report.category)) ? String(report.category) : 'other';
  const device = String(report.device ?? '').slice(0, 200);
  const contact = String(report.contact ?? '').trim().slice(0, MAX_CONTACT);

  // Same anti-spam rule as crash reports: store everything, email once.
  const seen = await sql`
    SELECT 1 FROM feedback
    WHERE message = ${message} AND device = ${device}
      AND created_at > now() - ${EMAIL_DEDUP_WINDOW}::interval
    LIMIT 1
  `;

  await sql`
    INSERT INTO feedback
      (category, message, contact, app_version, version_code, flavor,
       device, android_release, sdk_int, client_time, raw)
    VALUES (
      ${category}, ${message}, ${contact},
      ${String(report.app_version ?? '')}, ${Number(report.version_code ?? 0)},
      ${String(report.flavor ?? '')}, ${device},
      ${String(report.android_release ?? '')}, ${Number(report.sdk_int ?? 0)},
      ${Number(report.client_time ?? 0)}, ${JSON.stringify(report)}
    )
  `;

  let email = 'deduped';
  if (seen.length === 0) {
    // Email is best-effort; a mail failure must not reject the feedback.
    email = await notify({ ...report, category, message, contact, device }).catch(
      (e) => `error:${String(e).slice(0, 100)}`,
    );
  }

  return Response.json({ ok: true, email }, { status: 200 });
}

async function list(req: Request): Promise<Response> {
  const env = envCheck();
  if (!env.ok) return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });
  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureFeedbackTable(sql);

  const rows = await sql`
    SELECT id, created_at, category, message, contact, app_version,
           version_code, flavor, device, android_release, sdk_int, client_time
    FROM feedback
    ORDER BY created_at DESC
    LIMIT 200
  `;
  return Response.json({ feedback: rows });
}

async function notify(report: Feedback): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  const to = process.env.FEEDBACK_NOTIFY_EMAIL || process.env.CRASH_NOTIFY_EMAIL;
  if (!key || !to) return 'skipped:no-key-or-email';

  const subject =
    `[Nothing Modes feedback/${report.category}] ` +
    String(report.message).slice(0, 80);

  const text = [
    `Category:  ${report.category}`,
    `Contact:   ${report.contact || '-'}`,
    `App:       ${report.app_version ?? '?'} (${report.version_code ?? '?'}) / ${report.flavor ?? '?'}`,
    `Device:    ${report.device ?? '?'} — Android ${report.android_release ?? '?'} (SDK ${report.sdk_int ?? '?'})`,
    `Time:      ${report.client_time ? new Date(report.client_time).toISOString() : '-'}`,
    '',
    String(report.message),
  ].join('\n');

  const meta = (label: string, value: unknown) => `
    <tr>
      <td style="padding:4px 16px 4px 0;color:#8a8a8a;font-size:11px;letter-spacing:1px;white-space:nowrap;vertical-align:top;">${label}</td>
      <td style="padding:4px 0;color:#bbbbbb;font-size:13px;">${esc(String(value ?? '')) || '—'}</td>
    </tr>`;

  const html = nothingShell(
    'Feedback',
    `<tr><td style="padding:24px;">
      <p style="margin:0 0 6px;color:#D71921;font-size:11px;letter-spacing:2px;font-weight:700;">FEEDBACK · ${esc(String(report.category).toUpperCase())}</p>
      <table role="presentation" border="0" cellspacing="0" cellpadding="0" style="margin-bottom:16px;">
        ${meta('CONTACT', report.contact)}
        ${meta('APP', `${report.app_version ?? ''} (${report.version_code ?? ''}) · ${report.flavor ?? ''}`)}
        ${meta('DEVICE', `${report.device ?? ''} — Android ${report.android_release ?? ''} (SDK ${report.sdk_int ?? ''})`)}
      </table>
      <div style="border:1px solid #262626;border-radius:6px;background:#111111;padding:16px;color:#fafaf8;font-size:13px;line-height:1.6;white-space:pre-wrap;word-break:break-word;">${esc(String(report.message))}</div>
      <p style="margin:16px 0 0;color:#8a8a8a;font-size:11px;letter-spacing:1px;">All feedback lives at nothing-modes.vercel.app/admin</p>
    </td></tr>`,
  );

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${key}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: 'Nothing Modes <nothing-modes@tdvorak.dev>',
      to: [to],
      subject,
      text,
      html,
    }),
  });
  if (!res.ok) {
    const err = await res.text();
    return `failed:${res.status}:${err.slice(0, 150)}`;
  }
  return 'sent';
}
