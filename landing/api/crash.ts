import { neon } from '@neondatabase/serverless';

export const config = { runtime: 'edge' };

const MAX_BODY = 128 * 1024;
const EMAIL_DEDUP_WINDOW = "1 hour";

interface CrashReport {
  kind?: string;
  app?: string;
  app_version?: string;
  version_code?: number;
  flavor?: string;
  device?: string;
  android_release?: string;
  sdk_int?: number;
  exception_class?: string;
  message?: string;
  thread?: string;
  stacktrace?: string;
  context?: string;
  client_time?: number;
}

export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') {
    return new Response('method not allowed', { status: 405 });
  }

  const body = await req.text();
  if (body.length > MAX_BODY) {
    return new Response('payload too large', { status: 413 });
  }

  let report: CrashReport;
  try {
    report = JSON.parse(body);
  } catch {
    return new Response('invalid json', { status: 400 });
  }
  if (report?.app !== 'nothing-modes' || typeof report.stacktrace !== 'string') {
    return new Response('invalid report', { status: 400 });
  }

  const dbUrl = process.env.DATABASE_URL;
  if (!dbUrl) return new Response('server misconfigured', { status: 500 });
  const sql = neon(dbUrl);

  const exceptionClass = String(report.exception_class ?? '');
  const message = String(report.message ?? '');

  // Dedup check before insert so a crash loop doesn't spam email.
  const seen = await sql`
    SELECT 1 FROM crash_reports
    WHERE exception_class = ${exceptionClass} AND message = ${message}
      AND created_at > now() - ${EMAIL_DEDUP_WINDOW}::interval
    LIMIT 1
  `;

  await sql`
    INSERT INTO crash_reports
      (app_version, version_code, flavor, device, android_release, sdk_int,
       kind, exception_class, message, thread, stacktrace, context, raw)
    VALUES (
      ${String(report.app_version ?? '')}, ${Number(report.version_code ?? 0)},
      ${String(report.flavor ?? '')}, ${String(report.device ?? '')},
      ${String(report.android_release ?? '')}, ${Number(report.sdk_int ?? 0)},
      ${String(report.kind ?? 'crash')}, ${exceptionClass}, ${message},
      ${String(report.thread ?? '')}, ${report.stacktrace},
      ${String(report.context ?? '')}, ${JSON.stringify(report)}
    )
  `;

  let email = 'deduped';
  if (seen.length === 0) {
    // Email is best-effort; a mail failure must not reject the report.
    email = await notify(report).catch((e) => `error:${String(e).slice(0, 100)}`);
  }

  return Response.json({ ok: true, email }, { status: 200 });
}

async function notify(report: CrashReport): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  const to = process.env.CRASH_NOTIFY_EMAIL;
  if (!key || !to) return 'skipped:no-key-or-email';

  const subject =
    `[Nothing Modes ${report.kind ?? 'crash'}] ${report.exception_class ?? 'Error'}` +
    (report.message ? `: ${String(report.message).slice(0, 80)}` : '');

  const text = [
    `Kind:      ${report.kind ?? 'crash'}`,
    `App:       ${report.app_version ?? '?'} (${report.version_code ?? '?'}) / ${report.flavor ?? '?'}`,
    `Device:    ${report.device ?? '?'} — Android ${report.android_release ?? '?'} (SDK ${report.sdk_int ?? '?'})`,
    `Thread:    ${report.thread ?? '?'}`,
    `Context:   ${report.context || '-'}`,
    `Time:      ${report.client_time ? new Date(report.client_time).toISOString() : '-'}`,
    '',
    String(report.stacktrace ?? '').slice(0, 8000),
  ].join('\n');

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
      html: renderHtml(report, subject),
    }),
  });
  if (!res.ok) {
    const err = await res.text();
    return `failed:${res.status}:${err.slice(0, 150)}`;
  }
  return 'sent';
}

function esc(s: unknown): string {
  return String(s ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] ?? c,
  );
}

function row(label: string, value: unknown): string {
  return `<tr>
    <td style="padding:6px 12px;color:#888;font-size:11px;text-transform:uppercase;letter-spacing:.08em;white-space:nowrap;vertical-align:top">${label}</td>
    <td style="padding:6px 12px;color:#1a1a1a;font-size:13px">${esc(value) || '—'}</td>
  </tr>`;
}

function renderHtml(report: CrashReport, subject: string): string {
  const time = report.client_time ? new Date(report.client_time).toISOString() : '';
  return `<!DOCTYPE html><html><body style="margin:0;padding:0;background:#f4f4f4;font-family:ui-monospace,Menlo,Consolas,monospace">
  <div style="max-width:640px;margin:0 auto;padding:24px">
    <div style="background:#0a0a0a;border-radius:8px 8px 0 0;padding:20px 24px">
      <span style="color:#fff;font-size:14px;letter-spacing:.25em">N O T H I N G &nbsp; M O D E S</span>
      <span style="float:right;color:#D71921;font-size:11px;letter-spacing:.1em;font-weight:700">${esc(report.kind ?? 'crash').toUpperCase()}</span>
    </div>
    <div style="background:#fff;border:1px solid #e5e5e5;border-top:0;padding:8px 12px">
      <table style="width:100%;border-collapse:collapse">
        ${row('Exception', report.exception_class)}
        ${row('Message', report.message)}
        ${row('App', `${report.app_version ?? ''} (${report.version_code ?? ''}) · ${report.flavor ?? ''}`)}
        ${row('Device', `${report.device ?? ''} — Android ${report.android_release ?? ''} (SDK ${report.sdk_int ?? ''})`)}
        ${row('Thread', report.thread)}
        ${row('Context', report.context)}
        ${row('Time', time)}
      </table>
    </div>
    <div style="background:#fff;border:1px solid #e5e5e5;border-top:0;border-radius:0 0 8px 8px;padding:16px 24px">
      <div style="color:#888;font-size:11px;text-transform:uppercase;letter-spacing:.08em;margin-bottom:8px">Stack trace</div>
      <pre style="margin:0;padding:12px;background:#0f0f0f;color:#ccc;border-radius:6px;font-size:11px;line-height:1.5;overflow-x:auto;white-space:pre-wrap;word-break:break-all">${esc(String(report.stacktrace ?? '').slice(0, 8000))}</pre>
    </div>
    <p style="color:#aaa;font-size:10px;text-align:center;margin-top:16px">${esc(subject)} — view all reports at nothing-modes.vercel.app/admin</p>
  </div>
</body></html>`;
}
