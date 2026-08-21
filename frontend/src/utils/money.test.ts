import { describe, expect, it } from 'vitest';
import { formatINR, roundMoney } from './money';

describe('roundMoney', () => {
  it('corrects binary floating-point drift when summing line totals', () => {
    // The reason this function exists: 0.1 + 0.2 === 0.30000000000000004.
    expect(roundMoney(0.1 + 0.2)).toBe(0.3);
    expect(roundMoney(1.005 + 1.005)).toBe(2.01);
  });

  it('leaves clean amounts alone', () => {
    expect(roundMoney(1200)).toBe(1200);
    expect(roundMoney(0)).toBe(0);
  });
});

describe('formatINR', () => {
  it('groups in the Indian style, not the western one', () => {
    // 123456 is "1,23,456" in en-IN and "123,456" everywhere else. Getting this
    // wrong makes every price on the site look foreign to the customer.
    expect(formatINR(123456)).toBe('₹1,23,456');
  });

  it('drops the trailing .00 on whole amounts', () => {
    expect(formatINR(1200)).toBe('₹1,200');
  });

  it('keeps paise when there are any', () => {
    expect(formatINR(1200.5)).toBe('₹1,200.5');
  });

  it('handles zero', () => {
    expect(formatINR(0)).toBe('₹0');
  });
});
