const ALLOWED_DATABASE_NAMES = new Set(["financeapp_test", "fluxa_test"]);
const ALLOWED_HOSTS = new Set(["localhost", "127.0.0.1", "::1"]);

export function assertSafeTestDatabase(
  databaseUrl: string | undefined,
  databaseClient: string | undefined,
): void {
  if (databaseClient !== "pg") {
    throw new Error(
      "Test database guard: destructive integration tests require PostgreSQL.",
    );
  }

  if (!databaseUrl) {
    throw new Error("Test database guard: DATABASE_URL is required.");
  }

  let parsedUrl: URL;
  try {
    parsedUrl = new URL(databaseUrl);
  } catch {
    throw new Error(
      "Test database guard: DATABASE_URL must be a valid PostgreSQL URL.",
    );
  }

  const databaseName = decodeURIComponent(parsedUrl.pathname.replace(/^\//, ""));
  const isPostgresProtocol = ["postgres:", "postgresql:"].includes(
    parsedUrl.protocol,
  );

  if (
    !isPostgresProtocol ||
    !ALLOWED_HOSTS.has(parsedUrl.hostname) ||
    !ALLOWED_DATABASE_NAMES.has(databaseName)
  ) {
    throw new Error(
      "Test database guard: refusing destructive tests on a non-allowlisted database.",
    );
  }
}
