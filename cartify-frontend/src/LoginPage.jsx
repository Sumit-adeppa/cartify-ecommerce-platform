import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ShoppingBag, ShieldCheck } from "lucide-react";
import { useAuth } from "./AuthContext";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const user = await login(username.trim(), password);
      if (user.role === "ADMIN") {
        navigate("/admin", { replace: true });
      } else {
        const from = location.state?.from;
        const safeFrom = typeof from === "string" && from.startsWith("/") && !from.startsWith("//")
          ? from
          : "/";
        navigate(safeFrom, {
          replace: true,
          state: location.state?.pendingAddProductId != null
            ? { pendingAddProductId: location.state.pendingAddProductId }
            : null
        });
      }
    } catch (err) {
      setError(err.message || "Unable to sign in. Check your credentials.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-showcase">
        <Link className="brand brand-light" to="/"><span className="brand-mark"><ShoppingBag size={23}/></span>cartify</Link>
        <div className="showcase-copy">
          <span className="eyebrow">A better way to shop</span>
          <h1>Find what you love.<br/><em>Love what you find.</em></h1>
          <p>Everyday essentials and thoughtful finds, all in one place.</p>
        </div>
        <div className="showcase-note"><ShieldCheck size={17}/> Secure account access</div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <div className="mobile-brand brand"><span className="brand-mark"><ShoppingBag size={22}/></span>cartify</div>
          <span className="eyebrow">Welcome back</span>
          <h2>Sign in to Cartify</h2>
          <p className="muted">Enter your account details to continue shopping.</p>
          {error && <div className="alert error">{error}</div>}
          <form onSubmit={submit} className="form-stack">
            <label>Username<input value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" required placeholder="Your username"/></label>
            <label>Password<input type="password" value={password} onChange={event => setPassword(event.target.value)} autoComplete="current-password" required placeholder="Your password"/></label>
            <button className="btn btn-primary btn-full" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button>
          </form>
          <p className="auth-switch">New to Cartify? <Link to="/register" state={location.state}>Create an account</Link></p>
        </div>
      </section>
    </main>
  );
}
