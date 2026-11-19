import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Minus, Plus, Trash2, ShoppingBag } from "lucide-react";
import { Header } from "./Header";
import { api, money } from "./api";
import { useAuth } from "./AuthContext";

export default function CartPage() {
  const { user } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState("");

  async function loadCart() {
    setLoading(true); setError("");
    try { setCart(await api(`/api/cart/${user.userId}`)); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { loadCart(); }, [user?.userId]);

  const items = cart?.products || [];
  const total = cart?.overallTotalPrice ?? items.reduce((sum, item) => sum + Number(item.totalPrice || 0), 0);
  const count = cart?.totalItems ?? items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);

  async function update(productId, quantity) {
    if (quantity < 1) return remove(productId);
    setBusyId(productId); setError("");
    try {
      await api(`/api/cart/${user.userId}/items/${productId}`, { method: "PUT", body: JSON.stringify({ quantity }) });
      await loadCart();
    } catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }
  async function remove(productId) {
    setBusyId(productId); setError("");
    try {
      await api(`/api/cart/${user.userId}/items/${productId}`, { method: "DELETE" });
      await loadCart();
    } catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }
  async function checkout() {
    setError("");
    try {
      const order = await api(`/api/orders/${user.userId}`, { method: "POST" });
      const orderId = order?.orderId || order?.id;
      if (!orderId) throw new Error("Order was created, but the response did not contain orderId. Check Order Service response DTO.");
      const payment = await api(`/api/payment/create/${orderId}/user/${user.userId}`, { method: "POST" });
      const razorpayOrderId = payment?.razorpayOrderId || payment?.id;
      const amount = payment?.amount;
      const key = payment?.keyId || payment?.razorpayKeyId || import.meta.env.VITE_RAZORPAY_KEY_ID;
      if (!window.Razorpay || !razorpayOrderId || !amount || !key) {
        throw new Error("Order created. Payment response is missing Razorpay order ID, amount, or key ID. Verify the Payment Service response contract.");
      }
      const checkoutOptions = {
        key, amount, currency: payment.currency || "INR",
        name: "Cartify", description: `Payment for order ${orderId}`,
        order_id: razorpayOrderId,
        handler: async (result) => {
          try {
            await api(`/api/payment/verify/${user.userId}`, {
              method: "POST",
              body: JSON.stringify({
                razorpayOrderId: result.razorpay_order_id,
                razorpayPaymentId: result.razorpay_payment_id,
                razorpaySignature: result.razorpay_signature
              })
            });
            window.location.assign("/orders");
          } catch (err) { setError(`Payment verification failed: ${err.message}`); }
        },
        theme: { color: "#234b43" }
      };
      new window.Razorpay(checkoutOptions).open();
    } catch (err) { setError(err.message); }
  }

  return <div className="app-shell"><Header cartCount={count}/>
    <main className="content-wrap narrow-wrap">
      <Link to="/" className="back-link"><ArrowLeft size={16}/> Continue shopping</Link>
      <div className="section-heading cart-title"><div><span className="eyebrow">YOUR SELECTION</span><h2>Shopping cart</h2></div><span className="result-count">{count} items</span></div>
      {error && <div className="alert error">{error}</div>}
      {loading ? <div className="screen-center"><div className="spinner"/><p>Loading your cart…</p></div> :
        items.length === 0 ? <div className="empty-state"><ShoppingBag size={34}/><h3>Your cart is waiting</h3><p>Discover something useful and add it here.</p><Link to="/" className="btn btn-primary">Explore products</Link></div> :
        <div className="cart-layout">
          <section className="cart-list">{items.map(item => <article className="cart-row" key={item.productId}>
            <div className="cart-thumb">{item.imageUrl ? <img src={item.imageUrl} alt={item.name}/> : <ShoppingBag/>}</div>
            <div className="cart-product"><h3>{item.name}</h3><p>{money(item.pricePerUnit)} each</p><strong>{money(item.totalPrice)}</strong></div>
            <div className="quantity-stepper">
              <button aria-label="Decrease quantity" disabled={busyId === item.productId} onClick={() => update(item.productId, item.quantity - 1)}><Minus size={14}/></button>
              <span>{item.quantity}</span>
              <button aria-label="Increase quantity" disabled={busyId === item.productId} onClick={() => update(item.productId, item.quantity + 1)}><Plus size={14}/></button>
            </div>
            <button className="remove-button" aria-label="Remove item" onClick={() => remove(item.productId)}><Trash2 size={17}/></button>
          </article>)}</section>
          <aside className="summary-card"><span className="eyebrow">ORDER SUMMARY</span><h3>Ready when you are</h3><div className="summary-line"><span>Items ({count})</span><strong>{money(total)}</strong></div><div className="summary-line"><span>Shipping</span><strong>Calculated at checkout</strong></div><div className="summary-total"><span>Subtotal</span><strong>{money(total)}</strong></div><button className="btn btn-primary btn-full" onClick={checkout}>Continue to checkout</button><p className="summary-footnote">Your order total is calculated by Cartify's backend.</p></aside>
        </div>}
    </main>
  </div>;
}
