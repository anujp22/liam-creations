---
name: ui-ux
description: Practical UI/UX guardrails for web pages and components — responsive layout, accessibility (WCAG), visual hierarchy, typography, color/contrast, spacing, and interaction states. Use when building or restyling any user-facing screen, component, or form.
---

# UI/UX for Web

Applies to the React frontend and any HTML surface. The goal: clear, accessible,
consistent interfaces — not decoration for its own sake.

## Layout & responsiveness

- Mobile-first. Design the small screen first, then enhance for larger viewports.
- Use flexbox/grid and relative units (`rem`, `%`, `fr`, `clamp()`); avoid fixed
  pixel widths that break on small screens.
- Content never causes horizontal page scroll. Wide things (tables, code, wide
  images) scroll inside their own `overflow-x:auto` container.
- Respect a max content width for readability (~60–75ch for body text).

## Visual hierarchy

- One clear primary action per view; secondary actions visually subordinate.
- Establish a type scale (e.g. 1.25 ratio) and a spacing scale (4/8px based). Reuse
  them — don't invent one-off sizes.
- Group related elements with proximity and whitespace before reaching for borders.

## Color & contrast

- Text/background contrast meets WCAG AA (4.5:1 body, 3:1 large text / UI icons).
- Never encode meaning in color alone — pair with text, icon, or shape (colorblind
  users, error/success states).
- Support light and dark where the app does; test both.

## Typography

- System font stack or a single well-loaded webfont. Limit weights.
- Line-height ~1.5 for body. Don't justify text. Avoid all-caps for long strings.

## Accessibility (non-negotiable)

- Semantic HTML first (`button`, `nav`, `main`, `label`) before ARIA. A `div` with a
  click handler is not a button.
- Every input has an associated `<label>`. Every image has meaningful `alt` (or `alt=""`
  if decorative).
- Keyboard: everything interactive is focusable and operable by keyboard; visible focus
  ring; logical tab order; no keyboard traps.
- Respect `prefers-reduced-motion` for animations.

## Interaction states (design all of them)

- Buttons/links: default, hover, focus, active, disabled.
- Data views: loading (skeleton/spinner), error (message + retry), empty (guidance),
  success. Never a blank screen while loading.
- Forms: inline validation, clear error text near the field, disable submit while
  pending, confirm destructive actions.

## Feedback & performance perception

- Optimistic UI or immediate feedback for user actions; show progress for anything >1s.
- Lazy-load heavy images/routes; reserve space to avoid layout shift (CLS).

## Checklist

1. Works from ~360px wide up; no horizontal scroll.
2. Contrast passes AA; meaning isn't color-only.
3. Semantic HTML, labeled inputs, keyboard-operable, visible focus.
4. Loading / error / empty / disabled states all handled.
5. One clear primary action; consistent spacing/type scale.
