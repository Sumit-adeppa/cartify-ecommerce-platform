import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Package, ArrowLeft } from "lucide-react";
import { Header } from "./Header";
import { api, money } from "./api";
import { useAuth } from "./AuthContext";

export default function OrderPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api(`/api/orders/user/${user.userId}`)
      .then(data => setOrders(Array.isArray(data) ? data : data?.orders || []))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [user?.userId]);

  return <div className="app-shell"><Header/>
    <main className="content-wrap narrow-wrap">
      <Link to="/" className="back-link"><ArrowLeft size={16}/> Continue shopping</Link>
      <div className="section-heading"><div><span className="eyebrow">PURCHASE HISTORY</span><h2>Your orders</h2></div></div>
      {error && <div className="alert error">{error}</div>}
      {loading ? <div className="screen-center"><div className="spinner"/><p>Loading orders…</p></div> :
        orders.length === 0 ? <div className="empty-state"><Package size={34}/><h3>No orders yet</h3><p>Your completed purchases will appear here.</p><Link to="/" className="btn btn-primary">Start shopping</Link></div> :
        <div className="orders-stack">{orders.map(order => <article className="order-card" key={order.orderId}>
          <div className="order-top"><div><span className="eyebrow">ORDER</span><h3>{order.orderId}</h3></div><span className={`status-pill status-${String(order.status || "pending").toLowerCase()}`}>{order.status}</span></div>
          <div className="order-items">{(order.items || order.orderItems || []).map((item, index) => <div className="order-item" key={`${item.productId}-${index}`}><div><strong>{item.productName || item.name}</strong><span>Qty {item.quantity}</span></div><strong>{money(item.totalPrice)}</strong></div>)}</div>
          <div className="order-bottom"><span>{order.createdAt ? new Date(order.createdAt).toLocaleString() : "Order date unavailable"}</span><strong>{money(order.totalAmount)}</strong></div>
        </article>)}</div>}
    </main>
  </div>;
}
