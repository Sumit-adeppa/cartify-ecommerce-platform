import React, { useEffect, useState } from "react";
import { Header } from "./Header";
import { api, money, normalizeProducts } from "./api";

const localDate = (date = new Date()) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

// Convert supported date input formats to ISO yyyy-MM-dd without using
// Date.parse(), whose handling of dd-MM-yyyy is browser-dependent.
const normalizeDateInput = (value) => {
  if (typeof value !== "string") return null;
  const input = value.trim();
  let year, month, day;

  let match = input.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (match) {
    [, year, month, day] = match;
  } else {
    match = input.match(/^(\d{2})[-/](\d{2})[-/](\d{4})$/);
    if (!match) return null;
    // The displayed browser format in the supplied screenshot is dd-MM-yyyy.
    [, day, month, year] = match;
  }

  const iso = `${year}-${month}-${day}`;
  const parsed = new Date(`${iso}T00:00:00.000Z`);
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== iso) {
    return null;
  }
  return iso;
};

const emptyForm = {
  name: "", description: "", price: "", stock: "",
  categoryId: "", imageUrl: ""
};

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null);
  const today = localDate();
  const [analyticsFrom, setAnalyticsFrom] = useState(`${today.slice(0, 7)}-01`);
  const [analyticsTo, setAnalyticsTo] = useState(today);
  const [products, setProducts] = useState([]);
  const [userId, setUserId] = useState("");
  const [userResult, setUserResult] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [notice, setNotice] = useState("");
  const [productError, setProductError] = useState("");
  const [analyticsError, setAnalyticsError] = useState("");
  const [actionError, setActionError] = useState("");
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [loadingAnalytics, setLoadingAnalytics] = useState(true);

  async function loadProducts() {
    setLoadingProducts(true);
    setProductError("");
    try {
      const data = await api("/api/products");
      setProducts(normalizeProducts(data));
    } catch (err) {
      setProductError(err.message || "Unable to load products.");
    } finally {
      setLoadingProducts(false);
    }
  }

  async function loadAnalytics(from = analyticsFrom, to = analyticsTo) {
    setLoadingAnalytics(true);
    setAnalyticsError("");

    const normalizedFrom = normalizeDateInput(from);
    const normalizedTo = normalizeDateInput(to);

    if (!normalizedFrom || !normalizedTo) {
      setSummary(null);
      setAnalyticsError("Enter valid start and end dates.");
      setLoadingAnalytics(false);
      return;
    }

    // Compare canonical ISO date strings, not locale-formatted display strings.
    if (normalizedFrom > normalizedTo) {
      setSummary(null);
      setAnalyticsError("Start date must be on or before the end date.");
      setLoadingAnalytics(false);
      return;
    }

    // Keep date input state in the browser-required yyyy-MM-dd format.
    setAnalyticsFrom(normalizedFrom);
    setAnalyticsTo(normalizedTo);

    try {
      const params = new URLSearchParams({ from: normalizedFrom, to: normalizedTo });
      const data = await api(`/api/analytics/summary?${params.toString()}`);
      setSummary(data);
    } catch (err) {
      setSummary(null);
      setAnalyticsError(err.message || "Unable to load analytics.");
    } finally {
      setLoadingAnalytics(false);
    }
  }

  function applyAnalyticsDateRange(event) {
    event.preventDefault();
    loadAnalytics(analyticsFrom, analyticsTo);
  }

  async function load() {
    // Keep the two requests independent. An analytics failure must not hide products.
    await Promise.allSettled([loadProducts(), loadAnalytics()]);
  }

  useEffect(() => {
    load();
  }, []);

  async function addProduct(e) {
    e.preventDefault();
    setActionError("");
    setNotice("");
    try {
      await api("/admin/products/add", {
        method: "POST",
        body: JSON.stringify({
          name: form.name.trim(),
          description: form.description.trim(),
          price: Number(form.price),
          stock: Number(form.stock),
          categoryId: Number(form.categoryId),
          // AdminProductRequest on the backend expects imageUrl (String), not imageUrls (Array).
          imageUrl: form.imageUrl.trim()
        })
      });
      setNotice("Product created.");
      setForm(emptyForm);
      await loadProducts();
    } catch (err) {
      setActionError(err.message);
    }
  }

  async function deleteProduct(id) {
    if (!window.confirm(`Delete product ${id}?`)) return;
    setActionError("");
    setNotice("");
    try {
      await api("/admin/products/delete", {
        method: "DELETE",
        body: JSON.stringify({ productId: id })
      });
      setNotice("Product deleted.");
      await loadProducts();
    } catch (err) {
      setActionError(err.message);
    }
  }

  async function findUser(e) {
    e.preventDefault();
    setActionError("");
    setUserResult(null);
    try {
      setUserResult(
        await api(`/admin/user/getbyid?userId=${encodeURIComponent(userId)}`)
      );
    } catch (err) {
      setActionError(err.message);
    }
  }

  return (
    <div className="app-shell">
      <Header />
      <main className="content-wrap admin-wrap">
        <div className="section-heading">
          <div>
            <span className="eyebrow">CARTIFY MANAGEMENT</span>
            <h2>Admin dashboard</h2>
            <p className="muted">Manage catalogue and review store activity.</p>
          </div>
        </div>

        {notice && <div className="alert success">{notice}</div>}
        {actionError && <div className="alert error">{actionError}</div>}

        <section className="panel analytics-filter-panel">
          <span className="eyebrow">ANALYTICS REPORT</span>
          <h3>Select reporting period</h3>
          <form className="inline-form" onSubmit={applyAnalyticsDateRange}>
            <label>Start date
              <input
                type="date"
                required
                value={analyticsFrom}
                max={analyticsTo || today}
                onChange={event => setAnalyticsFrom(event.target.value)}
              />
            </label>
            <label>End date
              <input
                type="date"
                required
                value={analyticsTo}
                min={analyticsFrom}
                max={today}
                onChange={event => setAnalyticsTo(event.target.value)}
              />
            </label>
            <button className="btn btn-dark" type="submit" disabled={loadingAnalytics}>
              {loadingAnalytics ? "Loading…" : "Apply dates"}
            </button>
          </form>
          <p className="muted">Choose a start and end date to refresh the order and revenue summary.</p>
        </section>

        <section className="stats-grid">
          <div className="stat-card">
            <span>Total orders</span>
            <strong>{loadingAnalytics ? "Loading…" : summary?.totalOrders ?? "—"}</strong>
          </div>
          <div className="stat-card">
            <span>Successful orders</span>
            <strong>{loadingAnalytics ? "Loading…" : summary?.successfulOrders ?? "—"}</strong>
          </div>
          <div className="stat-card">
            <span>Pending orders</span>
            <strong>{loadingAnalytics ? "Loading…" : summary?.pendingOrders ?? "—"}</strong>
          </div>
          <div className="stat-card">
            <span>Revenue this month</span>
            <strong>{loadingAnalytics ? "Loading…" : summary ? money(summary.totalRevenue) : "—"}</strong>
          </div>
        </section>

        {analyticsError && (
          <div className="alert error">
            Analytics could not be loaded: {analyticsError}
            <button className="text-button" onClick={loadAnalytics}>Retry analytics</button>
          </div>
        )}

        <div className="admin-grid">
          <section className="panel">
            <span className="eyebrow">CATALOGUE</span>
            <h3>Add a product</h3>
            <form className="form-stack" onSubmit={addProduct}>
              <label>Product name
                <input required value={form.name}
                  onChange={e => setForm({ ...form, name: e.target.value })} />
              </label>
              <label>Description
                <textarea required value={form.description}
                  onChange={e => setForm({ ...form, description: e.target.value })} />
              </label>
              <div className="form-two">
                <label>Price (₹)
                  <input type="number" min="0.01" step="0.01" required value={form.price}
                    onChange={e => setForm({ ...form, price: e.target.value })} />
                </label>
                <label>Stock
                  <input type="number" min="0" required value={form.stock}
                    onChange={e => setForm({ ...form, stock: e.target.value })} />
                </label>
              </div>
              <div className="form-two">
                <label>Category ID
                  <input type="number" min="1" required value={form.categoryId}
                    onChange={e => setForm({ ...form, categoryId: e.target.value })} />
                </label>
                <label>Image URL
                  <input type="url" required value={form.imageUrl}
                    onChange={e => setForm({ ...form, imageUrl: e.target.value })} />
                </label>
              </div>
              <button className="btn btn-primary">Add product</button>
            </form>
          </section>

          <section className="panel">
            <span className="eyebrow">USER LOOKUP</span>
            <h3>Find a customer</h3>
            <form className="inline-form" onSubmit={findUser}>
              <input type="number" min="1" required placeholder="User ID"
                value={userId} onChange={e => setUserId(e.target.value)} />
              <button className="btn btn-dark">Find</button>
            </form>
            {userResult && (
              <div className="user-result">
                <strong>{userResult.username}</strong>
                <span>{userResult.email}</span>
                <span>Role: {userResult.role}</span>
                <span>ID: {userResult.userId}</span>
              </div>
            )}
          </section>
        </div>

        <section className="panel product-admin-panel">
          <span className="eyebrow">INVENTORY</span>
          <h3>Products</h3>
          {loadingProducts ? <p className="muted">Loading products…</p> :
            productError ? (
              <div className="alert error">
                Products could not be loaded: {productError}
                <button className="text-button" onClick={loadProducts}>Retry products</button>
              </div>
            ) : products.length === 0 ? (
              <p className="muted">No products returned by Product Service.</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr><th>Product</th><th>Price</th><th>Stock</th><th>Category</th><th></th></tr>
                  </thead>
                  <tbody>
                    {products.map(p => (
                      <tr key={p.productId}>
                        <td>{p.name}</td>
                        <td>{money(p.price)}</td>
                        <td>{p.stock}</td>
                        <td>{p.categoryName || p.categoryId}</td>
                        <td>
                          <button className="text-button danger-text"
                            onClick={() => deleteProduct(p.productId)}>Delete</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
        </section>
      </main>
    </div>
  );
}
