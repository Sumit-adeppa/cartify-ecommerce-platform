import React from "react";
import { ShoppingCart, Image as ImageIcon } from "lucide-react";
import { money } from "./api";

export function ProductList({ products, onAddToCart, addingId }) {
  if (!products.length) return <div className="empty-state"><ShoppingCart size={30}/><h3>No products found</h3><p>Products will appear here when they are available in the catalogue.</p></div>;
  return <div className="product-grid">
    {products.map(product => {
      const image = product.images?.[0] || product.imageUrl || "";
      const id = product.productId;
      return <article className="product-card" key={id}>
        <div className="product-image-wrap">
          {image ? <img src={image} alt={product.name} loading="lazy" onError={e => { e.currentTarget.style.display = "none"; }}/> : <div className="image-placeholder"><ImageIcon size={32}/></div>}
          {Number(product.stock) <= 0 && <span className="stock-tag">Out of stock</span>}
        </div>
        <div className="product-card-body">
          <span className="product-category">{product.categoryName || "Cartify selection"}</span>
          <h3>{product.name}</h3>
          <p>{product.description || "A useful addition to your everyday essentials."}</p>
          <div className="product-card-bottom"><strong>{money(product.price)}</strong>
            <button className="btn btn-dark btn-small" disabled={Number(product.stock) <= 0 || addingId === id} onClick={() => onAddToCart(id)}>
              <ShoppingCart size={15}/>{addingId === id ? "Adding…" : "Add"}
            </button>
          </div>
        </div>
      </article>;
    })}
  </div>;
}
