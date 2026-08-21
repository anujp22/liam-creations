import { Link } from 'react-router-dom';
import { useTitle } from '../hooks/useTitle';

/**
 * Required, not optional, from the moment orders started persisting a name, phone,
 * email and address.
 *
 * <p><strong>This copy is a draft and has not been reviewed by anyone qualified.</strong>
 * It describes what the code actually does, which is the part worth getting right first,
 * but the wording and the legal basis are the owner's to confirm. The contact address
 * below is a placeholder and must be replaced before this goes near a real domain —
 * a privacy policy with no working way to reach anyone is worse than none.
 */

// TODO(owner): replace with the real address for data requests, then delete this note.
const CONTACT_EMAIL = 'REPLACE-ME@example.com';
const INSTAGRAM_URL = 'https://www.instagram.com/_liamcreations';

export function PrivacyPolicyPage() {
  useTitle('Privacy Policy');

  return (
    <div className="legal-page">
      <h1 className="legal-title">Privacy Policy</h1>
      <p className="legal-updated">Last updated: 21 August 2026</p>

      <p>
        Liams Creations is a small business selling wedding essentials. This page explains
        what personal information we collect when you order from us, why we collect it,
        and what you can ask us to do with it.
      </p>

      <h2>What we collect</h2>
      <p>
        You do not need an account to browse or to order. We only collect information when
        you place an order, and only what we need to fulfil it:
      </p>
      <ul>
        <li>
          <strong>Your name</strong> — so we know who the order is for.
        </li>
        <li>
          <strong>Your phone number</strong> — this is how we confirm your order and reach
          you about delivery. Orders are arranged over WhatsApp.
        </li>
        <li>
          <strong>Your delivery address</strong> — so the order can be delivered.
        </li>
        <li>
          <strong>Your email address</strong> — optional. Leave it blank if you would
          rather we did not have it.
        </li>
        <li>
          <strong>Any note you add to your order</strong> — sizes, colours, delivery dates,
          whatever you tell us.
        </li>
        <li>
          <strong>What you ordered</strong> — the items, quantities and the prices at the
          time you ordered.
        </li>
      </ul>
      <p>
        Reviews you leave are stored with the rating and comment only. We do not attach
        your name or any contact details to a review.
      </p>

      <h2>What we do not collect</h2>
      <ul>
        <li>
          <strong>No payment details.</strong> There is no online payment on this site. We
          never see or store a card number.
        </li>
        <li>
          <strong>No accounts and no passwords</strong> for customers.
        </li>
        <li>
          <strong>No advertising or tracking cookies.</strong> Your cart and your saved
          delivery details are kept in your own browser's storage, on your device. They
          are not sent anywhere except when you place an order.
        </li>
      </ul>

      <h2>Why we keep it</h2>
      <p>
        To fulfil your order, to answer questions about it afterwards, and to keep a
        record of what was agreed and for how much. We do not sell your information, and
        we do not share it with anyone except where it is needed to deliver your order —
        for example, giving your address to a delivery service.
      </p>

      <h2>How long we keep it</h2>
      <p>
        We keep order records for as long as we may need them for the order itself and for
        our business records. If you would like your details removed sooner, ask us.
      </p>

      <h2>Analytics</h2>
      <p>
        We may use a privacy-friendly analytics service to count page views. It does not
        use cookies and does not build a profile of you or follow you across other sites.
      </p>

      <h2>Your choices</h2>
      <p>
        You can ask us to tell you what we hold about you, to correct it, or to delete it.
        Contact us and we will do it.
      </p>

      <h2>Contact</h2>
      <p>
        Email <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>, or message us on{' '}
        <a href={INSTAGRAM_URL} target="_blank" rel="noopener noreferrer">
          Instagram
        </a>
        .
      </p>

      <p className="legal-back">
        <Link to="/">← Back to the shop</Link>
      </p>
    </div>
  );
}
