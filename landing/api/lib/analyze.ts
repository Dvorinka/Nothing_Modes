/**
 * Static security analysis for user-submitted shared content.
 *
 * Shared items are pure data (automation JSON / glyph design JSON) — they can
 * never execute on the server — but they CAN be dangerous once installed:
 * a template could open a phishing URL, send an SMS to a premium number, or
 * write an arbitrary system setting. This analyzer enforces the rules below
 * BEFORE anything reaches review, and returns a sanitized copy of the payload
 * so personal data never hits the database.
 *
 * Verdicts:
 *   ok     — clean, safe to publish after review
 *   flag   — stored as pending but must be reviewed carefully (suspicious bits)
 *   reject — never stored; the submitter gets the findings back
 */

export interface Finding {
  severity: 'info' | 'warn' | 'block';
  code: string;
  detail: string;
}

export interface AnalysisResult {
  verdict: 'ok' | 'flag' | 'reject';
  findings: Finding[];
  /** Payload with personal data redacted — this is what gets stored/served. */
  sanitized: unknown;
  /** Human summary, e.g. "2 automations · 6 actions · needs Shizuku". */
  summary: string;
  capabilities: string[];
}

// ── Bounds ─────────────────────────────────────────────────────────────────

const LIMITS = {
  automations: 10,
  actionsPerAutomation: 20,
  conditionsDepth: 6,
  textLen: 500,
  urlLen: 2000,
  waitMsEach: 60_000,
  waitMsTotal: 300_000,
  vibrateMs: 10_000,
  countdownSec: 3600,
  volumeLevel: 30,
  brightness: 255,
  refreshHz: [24, 240] as const,
  glyphMatrixMax: 25 * 25,
  glyphFrames: 240,
};

// Action types that must never appear in published content.
const REJECTED_ACTIONS = new Set([
  'write_setting', // arbitrary system/secure/global writes — too powerful to share
]);

// Action types allowed only with constraints (checked below).
const KNOWN_ACTIONS = new Set([
  'set_wifi', 'set_bluetooth', 'set_mobile_data', 'set_dnd', 'set_ringer',
  'launch_app', 'open_url', 'show_notification', 'set_volume', 'set_flashlight',
  'set_dark_mode', 'open_settings_screen', 'vibrate', 'set_brightness',
  'set_auto_brightness', 'set_extra_dim', 'set_screen_timeout',
  'set_glyph', 'set_glyph_matrix', 'glyph_animate', 'glyph_progress',
  'glyph_text', 'glyph_scrolling_text', 'glyph_preset', 'glyph_turnoff',
  'glyph_icon', 'glyph_number', 'glyph_countdown', 'glyph_music',
  'copy_text', 'wait', 'set_auto_rotate', 'set_battery_saver',
  'set_airplane_mode', 'set_data_saver', 'set_hotspot', 'set_nfc',
  'set_refresh_rate', 'set_screen_rotation', 'media_control', 'send_sms',
  'lock_screen', 'set_location_mode', 'set_auto_sync', 'clear_notifications',
  'set_aod',
]);

const KNOWN_TRIGGERS = new Set([
  'time', 'time_window', 'immediate', 'manual', 'notification', 'phone_state',
  'connectivity', 'boot', 'battery_level', 'screen_state', 'app_opened',
  'geofence', 'bt_device', 'wifi_connected', 'calendar_event',
  'charger_connected', 'device_unlocked', 'device_locked',
]);

const SHIZUKU_ACTIONS = new Set([
  'set_wifi', 'set_bluetooth', 'set_mobile_data', 'set_extra_dim',
  'set_battery_saver', 'set_airplane_mode', 'set_data_saver', 'set_hotspot',
  'set_nfc', 'set_auto_sync', 'set_aod', 'set_location_mode',
]);

const GLYPH_ACTIONS = new Set([
  'set_glyph', 'set_glyph_matrix', 'glyph_animate', 'glyph_progress',
  'glyph_text', 'glyph_scrolling_text', 'glyph_preset', 'glyph_icon',
  'glyph_number', 'glyph_countdown', 'glyph_music', 'glyph_turnoff',
]);

// ── Helpers ────────────────────────────────────────────────────────────────

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v);
}

function typeOf(node: unknown): string {
  return isRecord(node) && typeof node.type === 'string' ? node.type : '';
}

function looksLikePhoneNumber(s: string): boolean {
  return /^\+?[0-9 ()-]{5,}$/.test(s.trim());
}

function validPkg(pkg: unknown): boolean {
  return typeof pkg === 'string' && /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$/.test(pkg);
}

function validHttpsUrl(url: unknown): boolean {
  if (typeof url !== 'string' || url.length > LIMITS.urlLen) return false;
  try {
    const u = new URL(url);
    return u.protocol === 'https:';
  } catch {
    return false;
  }
}

/** Recursively redact personal data in place: numbers, SSIDs, coordinates, calendar ids. */
function redact(node: unknown, findings: Finding[]) {
  if (!isRecord(node)) return;
  const t = typeOf(node);
  if (t === 'phone_state' && typeof node.number === 'string' && node.number) {
    node.number = '';
    findings.push({ severity: 'info', code: 'redact', detail: 'Redacted phone number from SMS/call trigger.' });
  }
  if ((t === 'wifi_connected' || t === 'connectivity') && typeof node.ssid === 'string' && node.ssid) {
    node.ssid = '';
    findings.push({ severity: 'info', code: 'redact', detail: 'Redacted Wi-Fi network name.' });
  }
  if (t === 'connectivity' && typeof node.match === 'string' && node.match) {
    node.match = '';
    findings.push({ severity: 'info', code: 'redact', detail: 'Redacted network/device match string.' });
  }
  if (t === 'bt_device' && (node.deviceName || node.deviceAddress)) {
    if (node.deviceAddress) node.deviceAddress = '';
    if (node.deviceName) node.deviceName = '';
    findings.push({ severity: 'info', code: 'redact', detail: 'Redacted Bluetooth device identity.' });
  }
  if (t === 'geofence') {
    if (node.lat || node.lng) {
      node.lat = 0; node.lng = 0;
      findings.push({ severity: 'warn', code: 'redact', detail: 'Redacted geofence coordinates — importer must set their own location.' });
    }
  }
  if (t === 'calendar_event' && node.calendarId) {
    node.calendarId = null;
    findings.push({ severity: 'info', code: 'redact', detail: 'Removed specific calendar id — importers pick their own calendar.' });
  }
  for (const v of Object.values(node)) {
    if (Array.isArray(v)) v.forEach((x) => redact(x, findings));
    else if (isRecord(v)) redact(v, findings);
  }
}

// ── Action checks ──────────────────────────────────────────────────────────

function checkAction(action: Record<string, unknown>, path: string, findings: Finding[], caps: Set<string>) {
  const t = typeOf(action);
  if (!t) {
    findings.push({ severity: 'block', code: 'shape', detail: `${path}: action missing type` });
    return;
  }
  if (REJECTED_ACTIONS.has(t)) {
    findings.push({ severity: 'block', code: 'forbidden_action', detail: `${path}: "${t}" is not allowed in shared content` });
    return;
  }
  if (!KNOWN_ACTIONS.has(t)) {
    findings.push({ severity: 'block', code: 'unknown_action', detail: `${path}: unknown action "${t}"` });
    return;
  }
  if (SHIZUKU_ACTIONS.has(t)) caps.add('shizuku_required');
  if (GLYPH_ACTIONS.has(t)) caps.add('glyph');

  switch (t) {
    case 'open_url':
      if (!validHttpsUrl(action.url)) {
        findings.push({ severity: 'block', code: 'url', detail: `${path}: only https:// URLs are allowed` });
      }
      break;
    case 'launch_app':
      if (!validPkg(action.pkg)) {
        findings.push({ severity: 'block', code: 'pkg', detail: `${path}: invalid package name` });
      }
      break;
    case 'send_sms': {
      const number = typeof action.number === 'string' ? action.number.trim() : '';
      if (looksLikePhoneNumber(number)) {
        findings.push({ severity: 'block', code: 'sms_number', detail: `${path}: SMS action may not carry a real number — leave it empty so the importer fills it in` });
      } else {
        findings.push({ severity: 'warn', code: 'sms', detail: `${path}: sends an SMS — review carefully` });
      }
      const text = typeof action.text === 'string' ? action.text : '';
      if (text.length > LIMITS.textLen) {
        findings.push({ severity: 'block', code: 'len', detail: `${path}: SMS text too long` });
      }
      caps.add('send_sms');
      break;
    }
    case 'wait': {
      const ms = Number(action.durationMs ?? 0);
      if (!(ms > 0 && ms <= LIMITS.waitMsEach)) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: wait must be 1ms–60s` });
      }
      break;
    }
    case 'vibrate': {
      const ms = Number(action.durationMs ?? 0);
      if (!(ms > 0 && ms <= LIMITS.vibrateMs)) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: vibration must be ≤10s` });
      }
      break;
    }
    case 'show_notification':
    case 'glyph_text':
    case 'glyph_scrolling_text':
    case 'copy_text': {
      const text = String(action.text ?? action.title ?? '');
      if (text.length > LIMITS.textLen) {
        findings.push({ severity: 'block', code: 'len', detail: `${path}: text over ${LIMITS.textLen} chars` });
      }
      break;
    }
    case 'set_glyph_matrix': {
      const colors = action.colors;
      if (Array.isArray(colors) && colors.length > LIMITS.glyphMatrixMax) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: matrix frame too large` });
      }
      break;
    }
    case 'glyph_countdown': {
      const s = Number(action.seconds ?? 0);
      if (!(s > 0 && s <= LIMITS.countdownSec)) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: countdown must be ≤60min` });
      }
      break;
    }
    case 'glyph_animate': {
      const cycles = Number(action.cycles ?? 0);
      const period = Number(action.periodMs ?? 0);
      if (cycles > 100 || period > 30_000 || period * cycles > LIMITS.waitMsTotal) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: animation too long` });
      }
      break;
    }
    case 'set_refresh_rate': {
      const hz = Number(action.hz ?? 0);
      if (hz < LIMITS.refreshHz[0] || hz > LIMITS.refreshHz[1]) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: refresh rate out of range` });
      }
      break;
    }
    case 'set_volume': {
      const level = Number(action.level ?? 0);
      if (level < 0 || level > LIMITS.volumeLevel) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: volume level out of range` });
      }
      break;
    }
    case 'set_brightness': {
      const level = Number(action.level ?? 0);
      if (level < 0 || level > LIMITS.brightness) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: brightness out of range` });
      }
      break;
    }
    case 'set_screen_timeout': {
      const ms = Number(action.timeoutMs ?? 0);
      if (ms < 0 || ms > 86_400_000) {
        findings.push({ severity: 'block', code: 'bounds', detail: `${path}: screen timeout out of range` });
      }
      break;
    }
    case 'lock_screen':
      caps.add('device_admin');
      break;
    case 'clear_notifications':
      caps.add('notification_listener');
      break;
  }
}

function checkTrigger(trigger: unknown, path: string, findings: Finding[], caps: Set<string>) {
  const t = typeOf(trigger);
  if (!KNOWN_TRIGGERS.has(t)) {
    findings.push({ severity: 'block', code: 'unknown_trigger', detail: `${path}: unknown trigger "${t || '?'}"` });
    return;
  }
  if (t === 'notification') caps.add('notification_listener');
  if (t === 'phone_state') caps.add('sms_or_phone');
  if (t === 'geofence') caps.add('location');
  if (t === 'calendar_event') caps.add('calendar');
  if (t === 'app_opened') caps.add('usage_access');
  if (t === 'bt_device') caps.add('bluetooth');
}

// ── Entry points ───────────────────────────────────────────────────────────

export function analyzeTemplate(payload: unknown): AnalysisResult {
  const findings: Finding[] = [];
  const caps = new Set<string>();
  const sanitized = structuredClone(payload) as Record<string, unknown>;

  if (!isRecord(sanitized) || !Array.isArray(sanitized.automations)) {
    return {
      verdict: 'reject', sanitized: payload, capabilities: [], summary: '',
      findings: [{ severity: 'block', code: 'shape', detail: 'payload must be an export bundle with an "automations" array' }],
    };
  }

  const autos = sanitized.automations as unknown[];
  if (autos.length === 0) {
    findings.push({ severity: 'block', code: 'empty', detail: 'bundle contains no automations' });
  }
  if (autos.length > LIMITS.automations) {
    findings.push({ severity: 'block', code: 'bounds', detail: `too many automations (${autos.length} > ${LIMITS.automations})` });
  }

  let actionCount = 0;
  let totalWait = 0;

  autos.forEach((a, i) => {
    const path = `automation[${i}]`;
    if (!isRecord(a)) {
      findings.push({ severity: 'block', code: 'shape', detail: `${path}: not an object` });
      return;
    }
    checkTrigger(a.trigger, `${path}.trigger`, findings, caps);

    const actions = Array.isArray(a.actions) ? a.actions : [];
    actionCount += actions.length;
    if (actions.length === 0) {
      findings.push({ severity: 'warn', code: 'empty', detail: `${path}: no actions — does nothing` });
    }
    if (actions.length > LIMITS.actionsPerAutomation) {
      findings.push({ severity: 'block', code: 'bounds', detail: `${path}: too many actions` });
    }
    actions.forEach((act, j) => {
      if (!isRecord(act)) {
        findings.push({ severity: 'block', code: 'shape', detail: `${path}.actions[${j}]: not an object` });
        return;
      }
      checkAction(act, `${path}.actions[${j}]`, findings, caps);
      if (typeOf(act) === 'wait') totalWait += Number(act.durationMs ?? 0);
    });
    if (a.conditions != null) {
      findings.push({ severity: 'info', code: 'conditions', detail: `${path}: has conditions — reviewed manually` });
    }
  });

  if (totalWait > LIMITS.waitMsTotal) {
    findings.push({ severity: 'block', code: 'bounds', detail: `total wait time ${Math.round(totalWait / 1000)}s exceeds 5min` });
  }

  // Strip the author's own profile from what we republish — attribution comes
  // from the submission fields, not embedded data.
  if (isRecord(sanitized.creatorProfile)) {
    sanitized.creatorProfile = {};
    findings.push({ severity: 'info', code: 'redact', detail: 'Embedded creator profile removed — attribution uses the publish form.' });
  }
  redact(sanitized, findings);

  const summary =
    `${autos.length} automation${autos.length === 1 ? '' : 's'} · ${actionCount} action${actionCount === 1 ? '' : 's'}` +
    (caps.has('shizuku_required') ? ' · needs Shizuku' : '') +
    (caps.has('glyph') ? ' · glyph' : '');

  const verdict = findings.some((f) => f.severity === 'block')
    ? 'reject'
    : findings.some((f) => f.severity === 'warn')
      ? 'flag'
      : 'ok';

  return { verdict, findings, sanitized, summary, capabilities: [...caps].sort() };
}

export function analyzeGlyph(payload: unknown): AnalysisResult {
  const findings: Finding[] = [];
  const caps = new Set<string>(['glyph']);
  const sanitized = structuredClone(payload);

  if (!isRecord(sanitized)) {
    return {
      verdict: 'reject', sanitized: payload, capabilities: [], summary: '',
      findings: [{ severity: 'block', code: 'shape', detail: 'glyph payload must be an object' }],
    };
  }

  const str = JSON.stringify(sanitized);
  if (str.length > 96 * 1024) {
    findings.push({ severity: 'block', code: 'size', detail: 'glyph payload over 96KB' });
  }

  // Open Glyph Museum format: { v, meta:{author,postId,url}, frames:[{d,p:[leds 0-255]}] }
  // `p` carries only the physical LEDs (489 for 25×25, 137 for 13×13).
  const v = Number(sanitized.v ?? 0);
  if (v !== 1 && v !== 4) {
    findings.push({ severity: 'warn', code: 'version', detail: `unrecognized format version v=${v || '?'} (expected 1 or 4)` });
  }
  const frames = sanitized.frames;
  if (!Array.isArray(frames) || frames.length === 0) {
    findings.push({ severity: 'block', code: 'shape', detail: 'no frames — not a valid glyph design' });
  } else {
    if (frames.length > LIMITS.glyphFrames) {
      findings.push({ severity: 'block', code: 'bounds', detail: `too many frames (${frames.length})` });
    }
    frames.forEach((f, i) => {
      if (!isRecord(f) || !Array.isArray(f.p)) {
        findings.push({ severity: 'block', code: 'shape', detail: `frame[${i}]: missing pixel array "p"` });
        return;
      }
      const p = f.p as unknown[];
      if (p.length === 0 || p.length > LIMITS.glyphMatrixMax || !p.every((x) => typeof x === 'number' && Number.isInteger(x) && x >= 0 && x <= 255)) {
        findings.push({ severity: 'block', code: 'bounds', detail: `frame[${i}]: "p" must be 1–625 ints 0–255` });
      }
      const d = f.d === undefined ? null : Number(f.d);
      if (d !== null && (!(d > 0) || d > 60_000)) {
        findings.push({ severity: 'block', code: 'bounds', detail: `frame[${i}]: duration "d" must be 1ms–60s` });
      }
    });
  }
  // meta.url is attribution (Glyph Museum link) — keep it, but it must be https.
  if (isRecord(sanitized.meta) && sanitized.meta.url !== undefined && sanitized.meta.url !== null && sanitized.meta.url !== '') {
    if (!validHttpsUrl(sanitized.meta.url)) {
      delete (sanitized.meta as Record<string, unknown>).url;
      findings.push({ severity: 'info', code: 'redact', detail: 'Removed non-https meta.url' });
    }
  }
  // URLs elsewhere in the payload are suspicious — designs are pure visuals.
  const withoutMeta = JSON.stringify({ ...sanitized, meta: undefined });
  if (/https?:\/\//i.test(withoutMeta)) {
    findings.push({ severity: 'warn', code: 'url', detail: 'glyph payload contains a URL outside meta — review' });
  }

  const name = typeof sanitized.name === 'string' ? sanitized.name : '';
  const n = Array.isArray(frames) ? frames.length : 0;
  const summary = `glyph design${name ? ` "${name.slice(0, 60)}"` : ''} · ${n} frame${n === 1 ? '' : 's'}`;

  const verdict = findings.some((f) => f.severity === 'block')
    ? 'reject'
    : findings.some((f) => f.severity === 'warn')
      ? 'flag'
      : 'ok';

  return { verdict, findings, sanitized, summary, capabilities: [...caps] };
}
