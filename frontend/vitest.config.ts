import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Kept separate from vite.config.ts: the build config has no business carrying test
// settings, and a `test` key declared there was silently not being applied.
export default defineConfig({
  plugins: [react()],
  test: {
    // Everything under test touches the DOM — rendering, clicking, localStorage.
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
});
