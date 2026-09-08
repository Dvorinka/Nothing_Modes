const REQUIRED = ['DATABASE_URL'];
const RECOMMENDED = ['RESEND_API_KEY', 'CRASH_NOTIFY_EMAIL'];

export function envCheck(): { ok: true } | { ok: false; missing: string[] } {
  const missing = REQUIRED.filter((k) => !process.env[k]);
  return missing.length === 0 ? { ok: true } : { ok: false, missing };
}

export function envWarnings(): string[] {
  return RECOMMENDED.filter((k) => !process.env[k]);
}
