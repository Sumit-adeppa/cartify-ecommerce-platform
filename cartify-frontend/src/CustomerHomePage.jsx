import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Search, SlidersHorizontal, Sparkles, ShoppingBag } from "lucide-react";
import { Header } from "./Header";
import { ProductList } from "./ProductList";
import { api, normalizeProducts } from "./api";
import { useAuth } from "./AuthContext";

export default function CustomerHomePage() {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [cartCount, setCartCount] = useState(0);
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("all");
  const [loading, setLoading] = useState(true);
  const [addingId, setAddingId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadProducts() {
    setLoading(true);
    setError("");
    try {
      const data = await api("/api/products");
      setProducts(normalizeProducts(data));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function loadCategories() {
    try {
      const data = await api("/api/categories");
      setCategories(Array.isArray(data) ? data : data?.categories || []);
    } catch {
      setCategories([]);
    }
  }

  const loadCartCount = useCallback(async () => {
    if (!user?.userId) {
      setCartCount(0);
      return;
    }
    try {
      const data = await api(`/api/cart/${user.userId}/count`);
      setCartCount(typeof data === "number" ? data : data?.count ?? data?.totalItems ?? 0);
    } catch {
      setCartCount(0);
    }
  }, [user?.userId]);

  useEffect(() => {
    loadProducts();
    loadCategories();
  }, []);

  useEffect(() => {
    loadCartCount();
  }, [loadCartCount]);

  const addToCart = useCallback(async (productId) => {
    if (!user) {
      navigate("/login", {
        state: {
          from: "/",
          pendingAddProductId: productId
        }
      });
      return;
    }

    setAddingId(productId);
    setMessage("");
    setError("");
    try {
      await api(`/api/cart/${user.userId}/items`, {
        method: "POST",
        body: JSON.stringify({ productId, quantity: 1 })
      });
      await loadCartCount();
      setMessage("Added to your cart.");
    } catch (err) {
      if (err.status === 401) {
        navigate("/login", {
          state: { from: "/", pendingAddProductId: productId }
        });
      } else {
        setError(err.message);
      }
    } finally {
      setAddingId(null);
    }
  }, [user, navigate, loadCartCount]);

  // After login, complete the Add to Cart action that caused the login redirect.
  useEffect(() => {
    const pendingProductId = location.state?.pendingAddProductId;
    if (user && pendingProductId != null) {
      navigate("/", { replace: true, state: null });
      addToCart(pendingProductId);
    }
  }, [user, location.state?.pendingAddProductId, addToCart, navigate]);

  const visible = useMemo(() => products.filter(product => {
    const text = `${product.name || ""} ${product.description || ""} ${product.categoryName || ""}`.toLowerCase();
    const matchesText = text.includes(query.toLowerCase());
    const matchesCategory = category === "all"
      || String(product.categoryId) === String(category)
      || product.categoryName === category;
    return matchesText && matchesCategory;
  }), [products, query, category]);

  return (
    <div className="app-shell">
      <Header cartCount={cartCount}/>
      <main className="content-wrap">
        <section className="hero-banner">
          <div>
            <span className="eyebrow"><Sparkles size={14}/> THE CARTIFY EDIT</span>
            <h1>Good finds.<br/><em>Better everyday.</em></h1>
            <p>Explore products picked to make your day a little easier.</p>
          </div>
          <div className="hero-orbit"><div className="hero-orbit-inner"><ShoppingBagGraphic/></div></div>
        </section>
        <section className="catalog-section">
          <div className="section-heading">
            <div><span className="eyebrow">YOUR NEXT FAVOURITE</span><h2>Shop the collection</h2></div>
            <span className="result-count">{visible.length} products</span>
          </div>
          <div className="catalog-toolbar">
            <div className="search-box"><Search size={18}/><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search products"/></div>
            <div className="filter-box">
              <SlidersHorizontal size={17}/>
              <select value={category} onChange={event => setCategory(event.target.value)}>
                <option value="all">All categories</option>
                {categories.map(item => <option key={item.categoryId ?? item.id} value={item.categoryId ?? item.id}>{item.categoryName ?? item.name}</option>)}
              </select>
            </div>
          </div>
          {message && <div className="alert success">{message}</div>}
          {error && <div className="alert error">{error}</div>}
          {loading
            ? <div className="screen-center"><div className="spinner"/><p>Loading products…</p></div>
            : <ProductList products={visible} onAddToCart={addToCart} addingId={addingId}/>}
        </section>
      </main>
      <footer className="site-footer">
        <span className="brand"><span className="brand-mark"><ShoppingBag size={17}/></span>cartify</span>
        <span>Thoughtful finds for everyday living.</span>
      </footer>
    </div>
  );
}

function ShoppingBagGraphic() {
  return (
    <svg viewBox="0 0 240 240" role="img" aria-label="Shopping bag illustration">
      <circle cx="120" cy="120" r="105" fill="#f3c9a8"/>
      <path d="M65 92h110l-9 105H74L65 92Z" fill="#234b43"/>
      <path d="M91 96V76a29 29 0 0 1 58 0v20" fill="none" stroke="#f8eadb" strokeWidth="9" strokeLinecap="round"/>
      <path d="M92 132c17 14 39 14 56 0" fill="none" stroke="#e6b78e" strokeWidth="7" strokeLinecap="round"/>
      <circle cx="179" cy="61" r="13" fill="#d87851"/>
    </svg>
  );
}
