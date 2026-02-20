// GraphQL client demos — field selection, mutations, introspection

'use strict';

const http  = require('http');
const { execFile, spawn } = require('child_process');
const path  = require('path');

const SERVER_URL  = 'http://localhost:3000/graphql';
const HEALTH_URL  = 'http://localhost:3000/health';
const SCRIPT_DIR  = __dirname;

// ---------------------------------------------------------------------------
// Minimal HTTP/GraphQL client (no extra deps beyond node-fetch)
// ---------------------------------------------------------------------------
let fetch;
try {
  fetch = require('node-fetch');
} catch (e) {
  console.error('[ERROR] node-fetch not found. Run: npm install');
  process.exit(1);
}

/**
 * Send a GraphQL request and return the parsed JSON response.
 * @param {string} query       - GraphQL query or mutation document
 * @param {object} [variables] - Optional variable map
 * @returns {Promise<object>}  - Full GraphQL response {data, errors}
 */
async function graphql(query, variables = {}) {
  const response = await fetch(SERVER_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, variables }),
  });
  return response.json();
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------
function separator(title = '', char = '=', width = 72) {
  if (title) {
    const padded = `  ${title}  `;
    const side = Math.floor((width - padded.length) / 2);
    console.log(char.repeat(side) + padded + char.repeat(side));
  } else {
    console.log(char.repeat(width));
  }
}

function printRequest(query, variables = {}) {
  console.log('\n[REQUEST]');
  console.log('  Method   : POST');
  console.log(`  URL      : ${SERVER_URL}`);
  console.log('  Headers  : Content-Type: application/json');
  console.log('  Body     :');
  console.log(JSON.stringify({ query: query.trim(), variables }, null, 4)
    .split('\n').map(l => '    ' + l).join('\n'));
}

function printResponse(data) {
  console.log('\n[RESPONSE]');
  console.log('  HTTP Status : 200 OK');
  console.log('  Body        :');
  console.log(JSON.stringify(data, null, 4)
    .split('\n').map(l => '    ' + l).join('\n'));
}

// ---------------------------------------------------------------------------
// Server lifecycle helpers
// ---------------------------------------------------------------------------
let serverProcess = null;

async function startServer() {
  return new Promise((resolve, reject) => {
    console.log('[SERVER] Starting Express/GraphQL server...');
    serverProcess = spawn('node', [path.join(SCRIPT_DIR, 'server.js')], {
      cwd: SCRIPT_DIR,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    serverProcess.stdout.on('data', d => process.stdout.write('[SERVER] ' + d));
    serverProcess.stderr.on('data', d => process.stderr.write('[SERVER] ' + d));

    // Poll health endpoint until server is ready
    let attempts = 0;
    const maxAttempts = 30;
    const interval = setInterval(async () => {
      attempts++;
      try {
        const res = await fetch(HEALTH_URL);
        if (res.ok) {
          clearInterval(interval);
          console.log(`[SERVER] Ready after ${attempts} attempt(s).`);
          resolve();
        }
      } catch (_) {
        if (attempts >= maxAttempts) {
          clearInterval(interval);
          reject(new Error('Server did not become ready within 15 seconds.'));
        }
      }
    }, 500);
  });
}

function stopServer() {
  if (serverProcess) {
    serverProcess.kill('SIGTERM');
    console.log('\n[SERVER] Stopped.');
  }
}

// ---------------------------------------------------------------------------
// Main demo
// ---------------------------------------------------------------------------
async function main() {
  console.log('=================  GraphQL Calculator Client — CS Assignment Demo  =================');
  console.log('Stack: Node.js + Express + express-graphql  (same as TELECOM SudParis lab)');
  console.log('Transport: HTTP  |  Format: JSON  |  Endpoint: POST /graphql\n');

  await startServer();

  // -------------------------------------------------------------------------
  // DEMO 1 — Field selection (key GraphQL advantage over REST)
  // -------------------------------------------------------------------------
  separator('DEMO 1 — Query with field selection (only result + operation)');
  console.log('\n  Key concept: the client declares exactly which fields it needs.');
  console.log('  Fields NOT listed are computed but never transmitted — no over-fetching.\n');

  const Q_SELECTIVE = `
    query AddSelective($a: Float!, $b: Float!, $op: String!) {
      calculate(a: $a, b: $b, operation: $op) {
        result
        operation
      }
    }
  `;
  const vars1 = { a: 15.5, b: 24.3, op: 'add' };
  printRequest(Q_SELECTIVE, vars1);
  const r1 = await graphql(Q_SELECTIVE, vars1);
  printResponse(r1);
  console.log(`\n  >>> Server returned ONLY 'result' and 'operation'.`);
  console.log(`  >>> Fields id, a, b, timestamp were NOT transmitted.`);
  console.log(`  >>> Result: ${r1.data.calculate.operation} -> ${r1.data.calculate.result}`);

  // -------------------------------------------------------------------------
  // DEMO 2 — Same query requesting ALL fields
  // -------------------------------------------------------------------------
  separator('DEMO 2 — Same query, all fields requested');
  const Q_ALL = `
    query DivideAll($a: Float!, $b: Float!, $op: String!) {
      calculate(a: $a, b: $b, operation: $op) {
        id
        operation
        a
        b
        result
        timestamp
      }
    }
  `;
  const vars2 = { a: 100.0, b: 4.0, op: 'divide' };
  printRequest(Q_ALL, vars2);
  const r2 = await graphql(Q_ALL, vars2);
  printResponse(r2);
  console.log(`\n  >>> All 6 fields returned this time. Result: ${r2.data.calculate.result}`);

  // -------------------------------------------------------------------------
  // DEMO 3 — Two more operations (SUBTRACT, MULTIPLY)
  // -------------------------------------------------------------------------
  separator('DEMO 3 — SUBTRACT and MULTIPLY operations');
  for (const [a, b, op] of [[50, 12.5, 'subtract'], [6, 7, 'multiply']]) {
    const r = await graphql(Q_SELECTIVE, { a, b, op });
    console.log(`  ${op.toUpperCase()}(${a}, ${b}) = ${r.data.calculate.result}`);
  }

  // -------------------------------------------------------------------------
  // DEMO 4 — Error handling (divide by zero)
  // -------------------------------------------------------------------------
  separator('DEMO 4 — Error handling: divide by zero');
  console.log('\n  Key concept: GraphQL errors appear in the "errors" array, NOT as HTTP 4xx.');
  console.log('  The HTTP status is still 200. This is a fundamental difference from REST.\n');

  const r4 = await graphql(Q_SELECTIVE, { a: 10.0, b: 0.0, op: 'divide' });
  printResponse(r4);
  if (r4.errors) {
    console.log(`\n  >>> HTTP 200 received but errors[] is populated: "${r4.errors[0].message}"`);
  }

  // -------------------------------------------------------------------------
  // DEMO 5 — History query
  // -------------------------------------------------------------------------
  separator('DEMO 5 — History query (retrieve all past calculations)');
  const Q_HISTORY = `
    query GetHistory($limit: Int) {
      history(limit: $limit) {
        id
        operation
        result
      }
    }
  `;
  printRequest(Q_HISTORY, { limit: 10 });
  const r5 = await graphql(Q_HISTORY, { limit: 10 });
  printResponse(r5);
  console.log(`\n  >>> Retrieved ${r5.data.history.length} record(s) from history.`);

  // -------------------------------------------------------------------------
  // DEMO 6 — Mutation (clearHistory)
  // -------------------------------------------------------------------------
  separator('DEMO 6 — Mutation: clearHistory');
  console.log('\n  Mutations are write operations — they change server state.');
  console.log('  Contrast with REST: POST/PUT/DELETE vs GraphQL mutations.\n');

  const M_CLEAR = `
    mutation {
      clearHistory
    }
  `;
  printRequest(M_CLEAR);
  const r6 = await graphql(M_CLEAR);
  printResponse(r6);
  console.log(`\n  >>> Deleted ${r6.data.clearHistory} record(s). History is now empty.`);

  // Verify
  const r6v = await graphql(Q_HISTORY, { limit: 10 });
  console.log(`  >>> Verification: history length after clear = ${r6v.data.history.length} (expected 0)`);

  // -------------------------------------------------------------------------
  // DEMO 7 — Introspection (self-describing schema)
  // -------------------------------------------------------------------------
  separator('DEMO 7 — Schema introspection (__schema)');
  console.log('\n  GraphQL schemas are fully introspectable — no external docs needed.');
  console.log('  This enables IDEs, code generators, and the GraphiQL interface.\n');

  const Q_INTRO = `
    query {
      __schema {
        types {
          name
          kind
        }
      }
    }
  `;
  const r7 = await graphql(Q_INTRO);
  const userTypes = r7.data.__schema.types
    .filter(t => !t.name.startsWith('__'))
    .map(t => `${t.name} (${t.kind})`);
  console.log('  Schema types (excluding built-in __meta types):');
  userTypes.forEach(t => console.log('    -', t));

  // -------------------------------------------------------------------------
  // Summary
  // -------------------------------------------------------------------------
  separator('Summary — GraphQL vs REST Key Differences');
  console.log(`
  Aspect              GraphQL                        REST
  ------------------------------------------------------------------
  Endpoint            Single /graphql                Multiple /resource/...
  HTTP method         Always POST                    GET/POST/PUT/DELETE
  Error signaling     errors[] in body (HTTP 200)    HTTP status codes 4xx/5xx
  Field control       Client selects fields           Server decides payload
  Schema              Introspectable via __schema     Depends on docs/OpenAPI
  Versioning          Schema deprecations            URL versioning /v1/ /v2/
  N+1 queries         Needs DataLoader / batching    Naturally per-endpoint
  `);

  console.log('\n  GraphiQL IDE (interactive browser playground): http://localhost:3000/graphql');

  stopServer();
  console.log('\nDemo complete.');
}

main().catch(err => {
  console.error('[FATAL]', err.message);
  stopServer();
  process.exit(1);
});
