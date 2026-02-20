// GraphQL calculator API — Express + express-graphql

const express = require('express');
const { graphqlHTTP } = require('express-graphql');
const { buildSchema } = require('graphql');

const PORT = 3000;

// ---------------------------------------------------------------------------
// GraphQL Schema (SDL)
// ---------------------------------------------------------------------------
// The schema defines all types, queries, and mutations the server exposes.
// Clients can ONLY request fields declared here — strong typing by design.
// This contrasts with REST where the response shape is fixed by the server.
const schema = buildSchema(`
  """
  Result of a single calculation operation.
  The client can request any combination of these fields — fields not
  requested are computed but never serialised (no over-fetching).
  """
  type CalculationResult {
    id: ID!
    operation: String!
    a: Float!
    b: Float!
    result: Float!
    timestamp: String!
  }

  """
  The root Query type.
  Queries are read-only — they do not change server state.
  """
  type Query {
    """
    Perform a calculation and return the result.
    operation: ADD | SUBTRACT | MULTIPLY | DIVIDE
    """
    calculate(a: Float!, b: Float!, operation: String!): CalculationResult

    """
    Retrieve the history of all past calculations.
    limit: maximum number of results to return (default: 10)
    """
    history(limit: Int): [CalculationResult]
  }

  """
  The root Mutation type.
  Mutations change server state — here we use them to clear the history,
  demonstrating the Query vs Mutation distinction.
  """
  type Mutation {
    """
    Clear the calculation history. Returns the number of records deleted.
    """
    clearHistory: Int
  }
`);

// ---------------------------------------------------------------------------
// In-memory store
// ---------------------------------------------------------------------------
const calculationHistory = [];
let idCounter = 1;

// ---------------------------------------------------------------------------
// Resolver functions
// ---------------------------------------------------------------------------
// Resolvers implement the logic behind each field in the schema.
// Each resolver receives (args, context, info) — we only use args here.
const root = {
  // --- Query: calculate ---
  calculate({ a, b, operation }) {
    const op = operation.toUpperCase();
    const allowed = ['ADD', 'SUBTRACT', 'MULTIPLY', 'DIVIDE'];

    if (!allowed.includes(op)) {
      throw new Error(
        `Unknown operation '${operation}'. Allowed: ${allowed.join(', ')}`
      );
    }

    if (op === 'DIVIDE' && b === 0) {
      // GraphQL errors are returned in the top-level "errors" array,
      // not as HTTP 4xx/5xx — the HTTP status code is still 200.
      // This is a key difference from REST error handling.
      throw new Error('Division by zero is undefined. Provide a non-zero divisor.');
    }

    const ops = { ADD: a + b, SUBTRACT: a - b, MULTIPLY: a * b, DIVIDE: a / b };
    const result = {
      id: String(idCounter++),
      operation: op,
      a,
      b,
      result: ops[op],
      timestamp: new Date().toISOString(),
    };

    calculationHistory.push(result);
    return result;
  },

  // --- Query: history ---
  history({ limit = 10 }) {
    return calculationHistory.slice(-limit);
  },

  // --- Mutation: clearHistory ---
  clearHistory() {
    const count = calculationHistory.length;
    calculationHistory.length = 0;
    idCounter = 1;
    return count;
  },
};

// ---------------------------------------------------------------------------
// Express app
// ---------------------------------------------------------------------------
const app = express();

// Single /graphql endpoint — ALL operations (queries + mutations) go here.
// This is the defining characteristic of GraphQL: one endpoint for everything.
// Compare: REST uses multiple URLs (/calculate, /history, etc.)
app.use(
  '/graphql',
  graphqlHTTP({
    schema,
    rootValue: root,
    graphiql: true,   // enable GraphiQL IDE at /graphql in the browser
  })
);

// Health check (used by client.js to wait for server readiness)
app.get('/health', (_req, res) => res.json({ status: 'ok' }));

const server = app.listen(PORT, () => {
  console.log(`[server] GraphQL server running on http://localhost:${PORT}/graphql`);
  console.log(`[server] Open in browser for GraphiQL IDE: http://localhost:${PORT}/graphql`);
});

// Export for testing / programmatic shutdown
module.exports = { app, server };
