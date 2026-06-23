/**
 * Money helpers. Prices are plain JS numbers (whole-rupee or 2-decimal),
 * so we round defensively to avoid binary floating-point drift when summing
 * line totals (e.g. 0.1 + 0.2), and format consistently with Indian grouping.
 */

/** Rounds an amount to 2 decimal places, correcting floating-point error. */
export function roundMoney(amount: number): number {
  return Math.round((amount + Number.EPSILON) * 100) / 100;
}

/** Formats a rupee amount: "₹1,23,456" (no trailing .00 for whole values). */
export function formatINR(amount: number): string {
  return `₹${roundMoney(amount).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}
