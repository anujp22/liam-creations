import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  error: Error | null;
}

/**
 * Catches render-time errors anywhere below it. Without this, a single thrown
 * error unmounts the whole tree and the customer is left staring at a white screen
 * with no way back.
 *
 * Must be a class component — React has no hook equivalent for componentDidCatch.
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // Keeps the stack in the browser console for debugging; the customer never sees it.
    console.error('Unhandled render error:', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;

    return (
      <div className="sale-page">
        <div className="sale-page-head">
          <h1 className="sale-page-title">Something went wrong</h1>
          <p className="sale-page-sub">
            Sorry — this page didn’t load properly. Reloading usually fixes it.
          </p>
        </div>

        <div className="grid-message">
          {/*
            A full reload rather than a router navigation: the component tree is in an
            unknown state, so tearing it down completely is the only reliable recovery.
          */}
          <button type="button" className="cart-back-link" onClick={() => window.location.reload()}>
            Reload the page
          </button>
          <p>
            <a href="/" className="cart-back-link">← Back to the catalog</a>
          </p>
        </div>
      </div>
    );
  }
}
