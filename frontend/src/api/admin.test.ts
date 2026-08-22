import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminAuthError, deleteProduct, fetchCurrentAdmin, login, logout } from './admin';

/**
 * The browser half of A12. The session cookie itself is untestable from here by
 * design — it is HttpOnly, which is the whole point — so what is worth pinning is the
 * CSRF token handling that cookies forced on us: get it wrong and every admin write
 * comes back 403, and there is no test in the backend suite that would notice, because
 * the backend is correct and it is this side that would be silently wrong.
 */

function mockFetch() {
  const fetchMock = vi.fn(async () => new Response(null, { status: 204 }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function lastInit(fetchMock: ReturnType<typeof mockFetch>): RequestInit {
  return fetchMock.mock.calls.at(-1)![1] as unknown as RequestInit;
}

function headerOf(init: RequestInit, name: string): string | null {
  return new Headers(init.headers).get(name);
}

describe('admin API session handling', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=tok%2Fen; path=/';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = 'XSRF-TOKEN=; path=/; max-age=0';
  });

  it('sends the CSRF token on a mutating request', async () => {
    const fetchMock = mockFetch();

    await deleteProduct('LC-0001');

    // Decoded: the cookie is URL-encoded on the way out and the header must not be.
    expect(headerOf(lastInit(fetchMock), 'X-XSRF-TOKEN')).toBe('tok/en');
  });

  it('does not send the CSRF token on a read', async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({ username: 'admin' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await fetchCurrentAdmin();

    expect(headerOf(lastInit(fetchMock), 'X-XSRF-TOKEN')).toBeNull();
  });

  it('sends cookies with admin requests', async () => {
    const fetchMock = mockFetch();

    await deleteProduct('LC-0001');

    // Without this the browser omits the session cookie and everything is a 401.
    expect(lastInit(fetchMock).credentials).toBe('same-origin');
  });

  it('logging in posts the credentials once and returns the username', async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({ username: 'admin' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    const name = await login('admin', 'secret');

    expect(name).toBe('admin');
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = lastInit(fetchMock);
    expect(init.body).toBe(JSON.stringify({ username: 'admin', password: 'secret' }));
    expect(headerOf(init, 'X-XSRF-TOKEN')).toBe('tok/en');
  });

  it('fetches a CSRF token first when the page has none yet', async () => {
    document.cookie = 'XSRF-TOKEN=; path=/; max-age=0';
    const fetchMock = vi.fn(async (url: string) => {
      if (url === '/api/admin/me') {
        document.cookie = 'XSRF-TOKEN=seeded; path=/';
        return new Response(null, { status: 401 });
      }
      return new Response(JSON.stringify({ username: 'admin' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    // Otherwise a login submitted before the provider's session check came back would
    // be rejected with an unexplained 403.
    await login('admin', 'secret');

    expect(fetchMock.mock.calls.map((c) => c[0])).toEqual(['/api/admin/me', '/api/admin/login']);
    expect(headerOf(lastInit(fetchMock), 'X-XSRF-TOKEN')).toBe('seeded');
  });

  it('reports bad credentials as an auth error, not a generic failure', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })));

    await expect(login('admin', 'wrong')).rejects.toBeInstanceOf(AdminAuthError);
  });

  it('never stores the password anywhere readable', async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({ username: 'admin' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    vi.stubGlobal('fetch', fetchMock);

    await login('admin', 'secret');

    // The old Basic scheme kept btoa('admin:secret') in sessionStorage and replayed it
    // on every request. Nothing may put it back.
    expect(JSON.stringify(sessionStorage)).not.toContain('secret');
    expect(JSON.stringify(localStorage)).not.toContain('secret');
  });

  it('logging out still resolves when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new Error('offline'); }));

    // The owner asked to leave; a network failure must not trap them in the admin.
    await expect(logout()).resolves.toBeUndefined();
  });
});
