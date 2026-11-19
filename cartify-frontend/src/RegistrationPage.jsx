import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ShoppingBag } from "lucide-react";
import { useAuth } from "./AuthContext";

export default function RegistrationPage() {
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const change = e => setForm({ ...form, [e.target.name]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await register(form);
      navigate("/login", { replace: true, state: { registered: true } });
    } catch (err) {
      setError(err.message || "Registration failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-layout">
      <section className="auth-showcase">
        <Link className="brand brand-light" to="/login"><span className="brand-mark"><ShoppingBag size={23}/></span>cartify</Link>
        <div className="showcase-copy"><span className="eyebrow">Join Cartify</span><h1>Your next<br/><em>great find</em> awaits.</h1><p>Create an account and make your everyday shopping simpler.</p></div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <div className="mobile-brand brand"><span className="brand-mark"><ShoppingBag size={22}/></span>cartify</div>
          <span className="eyebrow">Get started</span><h2>Create your account</h2><p className="muted">A few details and you're ready to go.</p>
          {error && <div className="alert error">{error}</div>}
          <form onSubmit={submit} className="form-stack">
            <label>Username<input name="username" value={form.username} onChange={change} minLength="3" maxLength="50" required autoComplete="username" placeholder="Choose a username"/></label>
            <label>Email address<input name="email" type="email" value={form.email} onChange={change} required autoComplete="email" placeholder="you@example.com"/></label>
            <label>Password<input name="password" type="password" value={form.password} onChange={change} minLength="8" required autoComplete="new-password" placeholder="At least 8 characters"/></label>
            <button className="btn btn-primary btn-full" disabled={busy}>{busy ? "Creating account…" : "Create account"}</button>
          </form>
          <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
        </div>
      </section>
    </main>
  );
}
