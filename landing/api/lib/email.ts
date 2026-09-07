const FROM = 'Nothing Modes <nothing-modes@tdvorak.dev>';

function esc(s: string): string {
  return String(s ?? '').replace(/[&<>"']/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] ?? c),
  );
}

function nothingShell(title: string, body: string): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)}</title>
</head>
<body style="margin:0;padding:0;background:#000000;color:#fafaf8;font-family:ui-monospace,Menlo,Consolas,monospace;">
  <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="background:#000000;">
    <tr>
      <td align="center" style="padding:28px 16px;">
        <table role="presentation" width="640" border="0" cellspacing="0" cellpadding="0" style="max-width:640px;width:100%;border:1px solid #262626;border-radius:8px;overflow:hidden;background:#0a0a0a;">
          <tr>
            <td style="padding:20px 24px;border-bottom:1px solid #262626;background:#0a0a0a;">
              <table role="presentation" border="0" cellspacing="0" cellpadding="0">
                <tr>
                  <td style="width:10px;height:10px;background:#D71921;border-radius:50%;padding:0;"></td>
                  <td style="padding-left:10px;font-size:12px;letter-spacing:3px;color:#fafaf8;font-weight:600;">NOTHING MODES</td>
                </tr>
              </table>
            </td>
          </tr>
          ${body}
          <tr>
            <td style="padding:18px 24px;border-top:1px solid #262626;color:#8a8a8a;font-size:11px;letter-spacing:1px;">
              OPEN SOURCE · GPL-3.0 · <a href="https://nothing-modes.vercel.app" style="color:#8a8a8a;text-decoration:underline;">nothing-modes.vercel.app</a>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
}

export async function notifyAdmin(
  to: string,
  sub: { id: string; type: string; title: string; handle: string; description: string },
  analysis: { verdict: string; findings: { severity: string; code: string; detail: string }[]; summary: string },
  itemUrl: string,
): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  if (!key || !to) return 'skipped:no-key-or-email';

  const findings = analysis.findings.length
    ? analysis.findings.map((f) => `<tr>
        <td style="padding:6px 0;vertical-align:top;width:64px;">
          <span style="font-size:10px;letter-spacing:1px;color:${f.severity === 'block' ? '#D71921' : f.severity === 'warn' ? '#e0a030' : '#8a8a8a'};font-weight:700;">${esc(f.severity.toUpperCase())}</span>
        </td>
        <td style="padding:6px 0;color:#bbbbbb;font-size:13px;">${esc(f.code)} — ${esc(f.detail)}</td>
      </tr>`).join('')
    : `<tr><td colspan="2" style="padding:6px 0;color:#8a8a8a;font-size:13px;">No findings.</td></tr>`;

  const body = `
          <tr>
            <td style="padding:24px;">
              <p style="margin:0 0 6px;color:#D71921;font-size:11px;letter-spacing:2px;font-weight:700;">NEW SUBMISSION</p>
              <h1 style="margin:0 0 14px;font-size:20px;font-weight:600;letter-spacing:1px;color:#fafaf8;">${esc(sub.title)}</h1>
              <p style="margin:0 0 18px;color:#bbbbbb;font-size:13px;line-height:1.55;">${esc(sub.description)}</p>
              <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="border:1px solid #262626;border-radius:6px;background:#111111;margin-bottom:18px;">
                <tr>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#8a8a8a;font-size:12px;letter-spacing:1px;">AUTHOR</td>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#fafaf8;font-size:13px;">@${esc(sub.handle)}</td>
                </tr>
                <tr>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#8a8a8a;font-size:12px;letter-spacing:1px;">TYPE</td>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#fafaf8;font-size:13px;">${esc(sub.type.toUpperCase())}</td>
                </tr>
                <tr>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#8a8a8a;font-size:12px;letter-spacing:1px;">ID</td>
                  <td style="padding:12px 16px;border-bottom:1px solid #262626;color:#fafaf8;font-size:13px;word-break:break-all;">${esc(sub.id)}</td>
                </tr>
                <tr>
                  <td style="padding:12px 16px;color:#8a8a8a;font-size:12px;letter-spacing:1px;vertical-align:top;">SUMMARY</td>
                  <td style="padding:12px 16px;color:#bbbbbb;font-size:13px;">${esc(analysis.summary)}</td>
                </tr>
              </table>
              <p style="margin:0 0 8px;color:#8a8a8a;font-size:11px;letter-spacing:1px;">STATIC ANALYSIS</p>
              <table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="border:1px solid #262626;border-radius:6px;background:#111111;padding:8px 16px;margin-bottom:24px;">
                ${findings}
              </table>
              <table role="presentation" border="0" cellspacing="0" cellpadding="0" style="width:100%;">
                <tr>
                  <td style="text-align:center;padding:0 6px;">
                    <a href="${esc(itemUrl)}" style="display:inline-block;background:#D71921;color:#ffffff;text-decoration:none;padding:12px 28px;border-radius:5px;font-size:13px;font-weight:600;letter-spacing:1px;">REVIEW IN ADMIN</a>
                  </td>
                </tr>
              </table>
            </td>
          </tr>`;

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      from: FROM,
      to: [to],
      subject: `Review pending — ${sub.title} by @${sub.handle}`,
      html: nothingShell('Submission review', body),
    }),
  });
  return res.ok ? 'sent' : `failed:${res.status}`;
}

export async function notifyAuthor(
  to: string,
  title: string,
  decision: 'approved' | 'rejected',
  reason: string,
): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  if (!key) return 'skipped:no-key';

  const approved = decision === 'approved';
  const body = `
          <tr>
            <td style="padding:24px;text-align:center;">
              <p style="margin:0 0 12px;color:${approved ? '#3fb950' : '#D71921'};font-size:11px;letter-spacing:2px;font-weight:700;">${approved ? 'PUBLISHED' : 'NOT PUBLISHED'}</p>
              <h1 style="margin:0 0 18px;font-size:20px;font-weight:600;letter-spacing:1px;color:#fafaf8;">${esc(title)}</h1>
              <p style="margin:0 0 24px;color:#bbbbbb;font-size:13px;line-height:1.55;">
                ${approved
                  ? `Your submission is now live in the <a href="https://nothing-modes.vercel.app/library" style="color:#D71921;text-decoration:none;">Nothing Modes Feature Library</a>.`
                  : 'Your submission was not published.'}
              </p>
              ${reason ? `<table role="presentation" width="100%" border="0" cellspacing="0" cellpadding="0" style="border:1px solid #262626;border-radius:6px;background:#111111;margin-bottom:24px;text-align:left;"><tr><td style="padding:16px;color:#bbbbbb;font-size:13px;line-height:1.5;">${esc(reason)}</td></tr></table>` : ''}
              ${approved
                ? `<a href="https://nothing-modes.vercel.app/library" style="display:inline-block;background:#D71921;color:#ffffff;text-decoration:none;padding:12px 28px;border-radius:5px;font-size:13px;font-weight:600;letter-spacing:1px;">OPEN LIBRARY</a>`
                : `<p style="margin:0;color:#8a8a8a;font-size:12px;">You can edit the content and submit again.</p>`}
            </td>
          </tr>`;

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      from: FROM,
      to: [to],
      subject: approved
        ? `Published — "${title}" is now in the Nothing Modes library`
        : `Not published — "${title}"`,
      html: nothingShell(approved ? 'Published' : 'Not published', body),
    }),
  });
  return res.ok ? 'sent' : `failed:${res.status}`;
}
