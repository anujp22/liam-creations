/**
 * "Our Story" — a small handwritten-style note tucked into the side gutter on the
 * homepage. Copy is seeded; the owner can rewrite or clear it. Shown only on wide
 * screens where there's gutter room beside the centered content.
 */
export function OurStoryNote() {
  return (
    <aside className="our-story-note" aria-label="Our story">
      <span className="our-story-tape" aria-hidden="true" />
      <h2 className="our-story-title">Our Story</h2>
      <p className="our-story-text">
        Every saree and décor piece is dreamt up and made by hand in our home
        studio — no factories, no shortcuts. Just hours of care poured into the
        details, so your forever day feels truly yours.
      </p>
      <p className="our-story-tagline">By hand, with love — for your forever day</p>
    </aside>
  );
}
