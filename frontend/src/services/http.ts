interface CsrfResponse {
  token: string;
  headerName: string;
}

let csrfResponse: Promise<CsrfResponse> | null = null;

async function getCsrfToken(): Promise<CsrfResponse> {
  if (!csrfResponse) {
    csrfResponse = fetch('/api/auth/csrf', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw new Error('Unable to initialize secure session.');
        return response.json() as Promise<CsrfResponse>;
      })
      .catch((error: unknown) => {
        csrfResponse = null;
        throw error;
      });
  }
  return csrfResponse;
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);

  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }

  return fetch(input, { ...init, headers, credentials: 'include' });
}

export function resetCsrfToken(): void {
  csrfResponse = null;
}