import http from 'k6/http';
import crypto from 'k6/crypto';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://app:8080';
const SECRET = __ENV.HMAC_SECRET || 'dev-secret-change-me';
const WRITE_RATIO = parseFloat(__ENV.WRITE_RATIO || '0');
const PLAYERS = 20000;
const DIFFS = ['easy', 'hard', 'boss'];
const MONSTERS = ['cindercub', 'tidepup', 'sproutle', 'zapret', 'fulgurr', 'vespike', 'gloomwisp', 'ironhorn',
    'golmoss', 'bulkox', 'zephyrax', 'sandrex', 'blazewing', 'krakoral', 'mossgore', 'oraclyx'];

export const options = {
    vus: parseInt(__ENV.VUS || '50', 10),
    duration: __ENV.DURATION || '30s',
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    thresholds: {
        'http_req_duration{name:top_overall}': [],
        'http_req_duration{name:top_difficulty}': [],
        'http_req_duration{name:player_rank}': [],
        'http_req_duration{name:submit}': [],
        'http_reqs{name:submit}': [],
    },
};

export function handleSummary(data) {
    const m = data.metrics;
    const row = (key, label) => {
        const v = m[key] && m[key].values;
        if (!v || v['p(95)'] === undefined || v.max === 0) return '';
        return `${label.padEnd(16)} p50=${v.med.toFixed(2)}ms p95=${v['p(95)'].toFixed(2)}ms p99=${v['p(99)'].toFixed(2)}ms\n`;
    };
    const failed = m.http_req_failed.values.rate * 100;
    const text = `RESULT reqs/s=${m.http_reqs.values.rate.toFixed(1)} failed=${failed.toFixed(2)}%\n`
        + row('http_req_duration', 'all')
        + row('http_req_duration{name:top_overall}', 'top_overall')
        + row('http_req_duration{name:top_difficulty}', 'top_difficulty')
        + row('http_req_duration{name:player_rank}', 'player_rank')
        + row('http_req_duration{name:submit}', 'submit');
    return { stdout: text };
}

const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];
const player = () => 'player' + String(1 + Math.floor(Math.random() * PLAYERS)).padStart(5, '0');

function submit() {
    const body = JSON.stringify({
        playerName: player(),
        monster: pick(MONSTERS),
        difficulty: pick(DIFFS),
        mode: 'quick',
        won: true,
        hpLeftPercent: 1 + Math.floor(Math.random() * 100),
        fightSeconds: 5 + Math.floor(Math.random() * 90),
        fightId: crypto.hexEncode(crypto.randomBytes(16)),
    });
    const ts = String(Math.floor(Date.now() / 1000));
    const sig = crypto.hmac('sha256', SECRET, `${ts}.${body}`, 'hex');
    const res = http.post(`${BASE}/api/scores`, body, {
        headers: { 'Content-Type': 'application/json', 'X-Timestamp': ts, 'X-Signature': sig },
        tags: { name: 'submit' },
    });
    check(res, { 'submit 201': (r) => r.status === 201 });
}

export default function () {
    if (Math.random() < WRITE_RATIO) {
        submit();
        return;
    }
    const q = Math.random();
    let res;
    if (q < 0.5) {
        res = http.get(`${BASE}/api/leaderboard/top?n=10`, { tags: { name: 'top_overall' } });
    } else if (q < 0.75) {
        res = http.get(`${BASE}/api/leaderboard/top?n=10&difficulty=${pick(DIFFS)}`, { tags: { name: 'top_difficulty' } });
    } else {
        res = http.get(`${BASE}/api/players/${player()}/rank`, { tags: { name: 'player_rank' } });
    }
    check(res, { 'read 200': (r) => r.status === 200 });
}
