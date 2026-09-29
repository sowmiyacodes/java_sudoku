#!/usr/bin/env node
/**
 * E2E flow test for the Sudoku project (backend on http://localhost:8080).
 *
 * Covers:
 *  1. Auth flow        — CSRF, register, login, profile
 *  2. Individual flow  — create Easy game, hint (backend solver), pause/resume,
 *                        undo/redo, wrong-move validation, early submit,
 *                        solve with independent solver, submit, leaderboard
 *  3. Multiplayer flow — create/join room, host-only start, shared board,
 *                        optimistic versioning (409 on stale), shared hint,
 *                        alternating moves by both players, submit, rewards
 *
 * Solves exactly 2 Easy puzzles: 1 individual + 1 multiplayer.
 */

const BASE = process.env.BASE_URL || 'http://localhost:8080';

// ---------------------------------------------------------------- results ---
let passCount = 0;
let failCount = 0;
const failures = [];

function step(title) {
  console.log(`\n=== ${title}`);
}

function check(name, cond, detail = '') {
  if (cond) {
    passCount++;
    console.log(`  PASS  ${name}${detail ? `  [${detail}]` : ''}`);
  } else {
    failCount++;
    failures.push(name + (detail ? ` — ${detail}` : ''));
    console.log(`  FAIL  ${name}${detail ? `  [${detail}]` : ''}`);
  }
}

// ----------------------------------------------------------------- solver ---
function isValidPlacement(b, row, col, num) {
  for (let i = 0; i < 9; i++) {
    if (b[row][i] === num || b[i][col] === num) return false;
  }
  const br = Math.floor(row / 3) * 3;
  const bc = Math.floor(col / 3) * 3;
  for (let r = 0; r < 3; r++)
    for (let c = 0; c < 3; c++)
      if (b[br + r][bc + c] === num) return false;
  return true;
}

/** Independent randomized backtracking solver (returns a new grid). */
function solveBoard(initial) {
  const b = initial.map((row) => row.slice());
  const rec = () => {
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        if (b[r][c] === 0) {
          for (let num = 1; num <= 9; num++) {
            if (isValidPlacement(b, r, c, num)) {
              b[r][c] = num;
              if (rec()) return true;
              b[r][c] = 0;
            }
          }
          return false;
        }
      }
    }
    return true;
  };
  return rec() ? b : null;
}

function isSolvedGrid(b) {
  const want = [1, 2, 3, 4, 5, 6, 7, 8, 9].join('');
  const line = (vals) => vals.slice().sort((a, c) => a - c).join('');
  for (let i = 0; i < 9; i++) {
    const row = [], col = [], box = [];
    for (let j = 0; j < 9; j++) {
      row.push(b[i][j]);
      col.push(b[j][i]);
      box.push(b[Math.floor(i / 3) * 3 + Math.floor(j / 3)][(i % 3) * 3 + (j % 3)]);
    }
    if (line(row) !== want || line(col) !== want || line(box) !== want) return false;
  }
  return true;
}

const countEmpty = (b) => b.flat().filter((v) => v === 0).length;

// ----------------------------------------------------------------- client ---
class Client {
  constructor(name) {
    this.name = name;
    this.cookies = new Map();
    this.csrf = null;
  }

  cookieHeader() {
    return [...this.cookies.entries()].map(([k, v]) => `${k}=${v}`).join('; ');
  }

  storeCookies(res) {
    for (const line of res.headers.getSetCookie?.() ?? []) {
      const [pair] = line.split(';');
      const idx = pair.indexOf('=');
      if (idx > 0) this.cookies.set(pair.slice(0, idx).trim(), pair.slice(idx + 1).trim());
    }
  }

  async refreshCsrf() {
    const res = await fetch(`${BASE}/api/auth/csrf`, {
      headers: { Cookie: this.cookieHeader() },
    });
    this.storeCookies(res);
    this.csrf = await res.json();
  }

  async request(path, { method = 'GET', body } = {}) {
    const mutating = !['GET', 'HEAD'].includes(method);
    // Refresh CSRF first: it may rotate the session cookie, so the Cookie
    // header must be built AFTER it (otherwise the POST carries a stale/empty
    // session and Spring returns 403).
    if (mutating) await this.refreshCsrf();
    const headers = { Cookie: this.cookieHeader() };
    if (mutating) headers[this.csrf.headerName] = this.csrf.token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    this.storeCookies(res);
    const text = await res.text();
    let json = null;
    try {
      json = JSON.parse(text);
    } catch {
      /* non-JSON body */
    }
    return { status: res.status, ok: res.ok, json, text };
  }

  get(path) {
    return this.request(path);
  }
  post(path, body) {
    return this.request(path, { method: 'POST', body });
  }
}

const rand = () => Math.random().toString(36).slice(2, 8);

// =========================================================================== //
async function main() {
  console.log('E2E FLOW TEST — target', BASE);

  // ---------------------------------------------------------------- health --
  step('0. Backend health');
  const health = await new Client('probe').get('/api/puzzles/generate?difficulty=Easy');
  check('puzzle generator responds', health.status === 200, `HTTP ${health.status}`);
  check('generated puzzle is Easy', health.json?.difficulty === 'Easy', health.json?.difficulty);
  check('puzzle has empty cells', countEmpty(health.json?.initialBoard ?? []) > 0);

  // =========================================================== INDIVIDUAL ==
  step('1. Individual flow — auth');
  const solo = new Client('solo');
  await solo.refreshCsrf();
  check('csrf token issued', Boolean(solo.csrf?.token), solo.csrf?.headerName);

  const uname = `e2e_solo_${rand()}`;
  const reg = await solo.post('/api/auth/register', {
    username: uname,
    displayName: `E2E Solo ${rand()}`,
    email: `${uname}@example.com`,
    password: 'Passw0rd!test',
  });
  check('register creates session', reg.status === 201, `HTTP ${reg.status}`);

  const me = await solo.get('/api/auth/me');
  check('profile reflects registered user', me.status === 200 && me.json?.username === uname, me.json?.username);

  const badLogin = await new Client('probe').post('/api/auth/login', {
    identifier: uname,
    password: 'wrong-password',
  });
  check('wrong password rejected', badLogin.status === 401, `HTTP ${badLogin.status}`);

  const goodLogin = await new Client('probe').post('/api/auth/login', {
    identifier: uname,
    password: 'Passw0rd!test',
  });
  check('login with username accepted', goodLogin.status === 200, `HTTP ${goodLogin.status}`);

  // ---------------------------------------------- anonymous individual game
  step('2. Individual flow — anonymous guest game (server allows public play)');
  const anon = new Client('anon');
  const anonGame = await anon.post('/api/games', { difficulty: 'Medium' });
  check('anonymous can create a game', anonGame.status === 201, `HTTP ${anonGame.status}`);

  // ------------------------------------------------------- create Easy game
  step('3. Individual flow — create EASY game (puzzle #1/2)');
  const created = await solo.post('/api/games', { difficulty: 'Easy' });
  check('game created', created.status === 201, `HTTP ${created.status}`);
  const game = created.json;
  check('game id assigned', Number.isInteger(game?.id), `id=${game?.id}`);
  check('game IN_PROGRESS', game?.status === 'IN_PROGRESS', game?.status);
  check('difficulty Easy', game?.difficulty === 'Easy', game?.difficulty);

  const empties = countEmpty(game?.initialBoard ?? []);
  check('Easy puzzle has 33–36 empty cells', empties >= 33 && empties <= 36, `${empties} empty`);
  check('current board equals initial board', JSON.stringify(game.board) === JSON.stringify(game.initialBoard));

  const solution = solveBoard(game.initialBoard);
  check('independent solver solves the puzzle', solution !== null && isSolvedGrid(solution));

  // -------------------------------------------------- backend solver (hint)
  step('4. Individual flow — backend solver via hint');
  const hint = await solo.post(`/api/games/${game.id}/hint?level=3`);
  check('hint available', hint.status === 200 && hint.json?.available === true, `HTTP ${hint.status}`);
  const h = hint.json;
  check(
    'hint value matches independent solution',
    h?.available && game.initialBoard[h.row][h.column] === 0 && solution[h.row][h.column] === h.value,
    h?.available ? `(${h.row},${h.column})=${h.value} technique=${h.technique}` : 'no hint'
  );

  // ------------------------------------------------------ pause / resume
  step('5. Individual flow — pause / resume');
  const paused = await solo.post(`/api/games/${game.id}/pause`);
  check('pause works', paused.status === 200 && paused.json?.status === 'PAUSED', paused.json?.status);

  const firstEmpty = (() => {
    for (let r = 0; r < 9; r++) for (let c = 0; c < 9; c++) if (game.initialBoard[r][c] === 0) return [r, c];
    return [0, 0];
  })();
  const pausedMove = await solo.post(`/api/games/${game.id}/move`, {
    row: firstEmpty[0],
    column: firstEmpty[1],
    value: solution[firstEmpty[0]][firstEmpty[1]],
  });
  check('move rejected while paused', pausedMove.status === 400, `HTTP ${pausedMove.status}`);

  const active = await solo.get('/api/games/active');
  check('active (resumable) game visible while paused', active.status === 200 && active.json?.id === game.id, `HTTP ${active.status}`);

  const resumed = await solo.post(`/api/games/${game.id}/resume`);
  check('resume works', resumed.status === 200 && resumed.json?.status === 'IN_PROGRESS', resumed.json?.status);

  // -------------------------------------------------- negative submit early
  step('6. Individual flow — early submit & wrong move validation');
  const early = await solo.post(`/api/games/${game.id}/submit`);
  check('early submit reports incomplete', early.status === 200 && early.json?.completed === false, early.json?.message);
  check('early submit counts empty cells', early.json?.emptyCells === empties, `emptyCells=${early.json?.emptyCells}`);

  // wrong value on an empty cell (rule-breaking) -> mistake counted, board unchanged
  const wrong = await solo.post(`/api/games/${game.id}/move`, {
    row: firstEmpty[0],
    column: firstEmpty[1],
    value: ((solution[firstEmpty[0]][firstEmpty[1]] % 9) + 1),
  });
  check('wrong move flagged invalid', wrong.json?.valid === false, wrong.json?.message);
  check('mistake recorded', (wrong.json?.mistakes ?? 0) >= 1, `mistakes=${wrong.json?.mistakes}`);

  const fixedCell = await solo.post(`/api/games/${game.id}/move`, { row: 0, column: 0, value: 5 });
  check('editing a fixed clue rejected', fixedCell.json?.valid === false, fixedCell.json?.message);

  // --------------------------------------------------------- undo / redo
  step('7. Individual flow — undo / redo');
  const goodMove = await solo.post(`/api/games/${game.id}/move`, {
    row: firstEmpty[0],
    column: firstEmpty[1],
    value: solution[firstEmpty[0]][firstEmpty[1]],
  });
  check('correct move accepted', goodMove.json?.valid === true, `solved=${goodMove.json?.solved}`);

  const undone = await solo.post(`/api/games/${game.id}/undo`);
  check('undo clears the cell', undone.status === 200 && undone.json?.board[firstEmpty[0]][firstEmpty[1]] === 0, `HTTP ${undone.status}`);

  const redone = await solo.post(`/api/games/${game.id}/redo`);
  check('redo restores the cell', redone.status === 200 && redone.json?.board[firstEmpty[0]][firstEmpty[1]] === solution[firstEmpty[0]][firstEmpty[1]], `HTTP ${redone.status}`);

  // ---------------------------------------------- solve the rest & submit
  step('8. Individual flow — solve EASY puzzle #1/2 and submit');
  let movesSent = 0;
  for (let r = 0; r < 9; r++) {
    for (let c = 0; c < 9; c++) {
      if (game.initialBoard[r][c] !== 0) continue;
      if (r === firstEmpty[0] && c === firstEmpty[1]) continue; // already placed by redo
      const mv = await solo.post(`/api/games/${game.id}/move`, { row: r, column: c, value: solution[r][c] });
      if (mv.json?.valid !== true) {
        check(`move (${r},${c})=${solution[r][c]} accepted`, false, mv.json?.message || `HTTP ${mv.status}`);
      }
      movesSent++;
    }
  }
  check(`all ${movesSent} remaining moves accepted`, true);

  const finalState = await solo.get(`/api/games/${game.id}`);
  check('board fully solved', isSolvedGrid(finalState.json?.board ?? []));
  check('game auto-completes when solved', finalState.json?.status === 'COMPLETED', finalState.json?.status);

  const submitted = await solo.post(`/api/games/${game.id}/submit`);
  check('submit succeeds', submitted.status === 200 && submitted.json?.completed === true && submitted.json?.valid === true, submitted.json?.message);
  check('submit awards points', typeof submitted.json?.pointsAwarded === 'number' && submitted.json.pointsAwarded > 0, `points=${submitted.json?.pointsAwarded}`);
  check('mistakes carried into submit', submitted.json?.mistakes >= 1, `mistakes=${submitted.json?.mistakes}`);

  const noActive = await solo.get('/api/games/active');
  check('no active game after completion (204)', noActive.status === 204, `HTTP ${noActive.status}`);

  const boardMe = await solo.get('/api/leaderboard/me');
  check('leaderboard/me returns profile score', boardMe.status === 200, `HTTP ${boardMe.status}`);
  console.log('        leaderboard/me:', JSON.stringify(boardMe.json).slice(0, 300));

  // ========================================================= MULTIPLAYER ==
  step('9. Multiplayer — auth guard');
  const anon2 = new Client('anon2');
  const anonRoom = await anon2.post('/api/rooms', { difficulty: 'Easy' });
  check('anonymous cannot create room (401)', anonRoom.status === 401, `HTTP ${anonRoom.status}`);

  const host = new Client('host');
  const guest = new Client('guest');
  const stranger = new Client('stranger');
  await host.refreshCsrf();
  await guest.refreshCsrf();
  await stranger.refreshCsrf();

  const hName = `e2e_host_${rand()}`;
  const gName = `e2e_guest_${rand()}`;
  const sName = `e2e_str_${rand()}`;
  const hReg = await host.post('/api/auth/register', {
    username: hName, displayName: 'E2E Host', email: `${hName}@example.com`, password: 'Passw0rd!test',
  });
  const gReg = await guest.post('/api/auth/register', {
    username: gName, displayName: 'E2E Guest', email: `${gName}@example.com`, password: 'Passw0rd!test',
  });
  const sReg = await stranger.post('/api/auth/register', {
    username: sName, displayName: 'E2E Stranger', email: `${sName}@example.com`, password: 'Passw0rd!test',
  });
  check('host registered', hReg.status === 201, `HTTP ${hReg.status}`);
  check('guest registered', gReg.status === 201, `HTTP ${gReg.status}`);
  check('stranger registered', sReg.status === 201, `HTTP ${sReg.status}`);

  step('10. Multiplayer — room lifecycle');
  const createdRoom = await host.post('/api/rooms', { difficulty: 'Easy' });
  check('room created (201)', createdRoom.status === 201, `HTTP ${createdRoom.status}`);
  const code = createdRoom.json?.roomCode;
  check('room code issued', typeof code === 'string' && code.length === 6, `code=${code}`);
  check('room WAITING', createdRoom.json?.status === 'WAITING', createdRoom.json?.status);
  check('creator is HOST', createdRoom.json?.you?.role === 'HOST', createdRoom.json?.you?.role);

  const strangerView = await stranger.get(`/api/rooms/${code}`);
  check('non-member cannot poll room (403)', strangerView.status === 403, `HTTP ${strangerView.status}`);

  const badCode = await guest.post('/api/rooms/join', { roomCode: 'ZZZZZZ' });
  check('invalid room code → 404', badCode.status === 404, `HTTP ${badCode.status}`);

  const joined = await guest.post('/api/rooms/join', { roomCode: code });
  check('guest joins room', joined.status === 200, `HTTP ${joined.status}`);
  check('two players present', joined.json?.players?.length === 2, `players=${joined.json?.players?.length}`);
  check('guestUsername set', Boolean(joined.json?.guestUsername), joined.json?.guestUsername);

  const guestStart = await guest.post(`/api/rooms/${code}/start`);
  check('guest cannot start (403)', guestStart.status === 403, `HTTP ${guestStart.status}`);

  const started = await host.post(`/api/rooms/${code}/start`);
  check('host starts the shared game', started.status === 200 && started.json?.status === 'IN_PROGRESS', started.json?.status || `HTTP ${started.status}`);
  const roomGame = started.json?.game;
  check('shared game created (Easy)', roomGame?.difficulty === 'Easy', roomGame?.difficulty);
  check('shared game has empty cells', countEmpty(roomGame?.initialBoard ?? []) > 0);

  const hostView = await host.get(`/api/rooms/${code}`);
  const guestView = await guest.get(`/api/rooms/${code}`);
  check('host polls state', hostView.status === 200 && hostView.json?.game?.id === roomGame?.id, `HTTP ${hostView.status}`);
  check('guest polls identical shared game', guestView.status === 200 && guestView.json?.game?.id === roomGame?.id, `HTTP ${guestView.status}`);

  step('11. Multiplayer — backend solver via shared hint');
  const solution2 = solveBoard(roomGame.initialBoard);
  check('independent solver solves room puzzle', solution2 !== null && isSolvedGrid(solution2));
  const roomHint = await guest.post(`/api/rooms/${code}/hint?level=3`);
  check('shared hint available', roomHint.status === 200 && roomHint.json?.available === true, `HTTP ${roomHint.status}`);
  check(
    'shared hint matches independent solution',
    roomHint.json?.available && solution2[roomHint.json.row][roomHint.json.column] === roomHint.json.value,
    roomHint.json?.available ? `(${roomHint.json.row},${roomHint.json.column})=${roomHint.json.value}` : 'no hint'
  );

  step('12. Multiplayer — solve EASY puzzle #2/2 with alternating moves');
  let version = started.json?.stateVersion;
  const roomEmpties = [];
  for (let r = 0; r < 9; r++)
    for (let c = 0; c < 9; c++)
      if (roomGame.initialBoard[r][c] === 0) roomEmpties.push([r, c]);

  // stale-version negative check first
  const stale = await host.post(`/api/rooms/${code}/move`, {
    row: roomEmpties[0][0], column: roomEmpties[0][1], value: solution2[roomEmpties[0][0]][roomEmpties[0][1]],
    expectedStateVersion: 0,
  });
  check('stale expectedStateVersion rejected (409)', stale.status === 409, `HTTP ${stale.status}`);

  let moveErrors = 0;
  for (let i = 0; i < roomEmpties.length; i++) {
    const [r, c] = roomEmpties[i];
    const client = i % 2 === 0 ? host : guest; // alternate host/guest
    const mv = await client.post(`/api/rooms/${code}/move`, {
      row: r, column: c, value: solution2[r][c], expectedStateVersion: version,
    });
    if (mv.status !== 200 || mv.json?.move?.valid === false) {
      moveErrors++;
      check(`room move #${i} (${r},${c})`, false, `HTTP ${mv.status} ${JSON.stringify(mv.json).slice(0, 150)}`);
    }
    version = mv.json?.stateVersion ?? version;
  }
  check(`all ${roomEmpties.length} shared moves accepted (alternating host/guest)`, moveErrors === 0, `${moveErrors} errors`);

  const roomState = await host.get(`/api/rooms/${code}`);
  check('shared board fully solved', isSolvedGrid(roomState.json?.game?.board ?? []));
  check('game auto-completes when solved', roomState.json?.game?.status === 'COMPLETED', roomState.json?.game?.status);

  step('13. Multiplayer — submit & rewards');
  const roomSubmit = await guest.post(`/api/rooms/${code}/submit`);
  check('room submit succeeds', roomSubmit.status === 200 && roomSubmit.json?.completed === true, roomSubmit.json?.message);
  console.log('        submit:', JSON.stringify(roomSubmit.json));

  const finalRoom = await guest.get(`/api/rooms/${code}`);
  check('room status COMPLETED', finalRoom.json?.status === 'COMPLETED', finalRoom.json?.status);
  const points = (finalRoom.json?.players ?? []).map((p) => p.pointsAwarded);
  check('both players awarded points', points.length === 2 && points.every((p) => typeof p === 'number' && p > 0), JSON.stringify(points));

  const reSubmit = await host.post(`/api/rooms/${code}/submit`);
  check('submit idempotent for finished room', reSubmit.status === 200 && reSubmit.json?.completed === true, `HTTP ${reSubmit.status}`);

  // ------------------------------------------------------------- summary --
  step('SUMMARY');
  console.log(`  passed: ${passCount}`);
  console.log(`  failed: ${failCount}`);
  if (failures.length) {
    console.log('  failures:');
    for (const f of failures) console.log(`   - ${f}`);
  }
  process.exit(failCount === 0 ? 0 : 1);
}

main().catch((err) => {
  console.error('FATAL:', err);
  process.exit(2);
});
