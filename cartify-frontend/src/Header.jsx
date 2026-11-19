import { Link, NavLink, useNavigate } from "react-router-dom";
import { ShoppingBag, ShoppingCart, UserRound, LogOut, Package } from "lucide-react";
import { useAuth } from "./AuthContext";

export function Header({ cartCount = 0 }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function signOut() {
    await logout();
    navigate("/", { replace: true });
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link to={user?.role === "ADMIN" ? "/admin" : "/"} className="brand">
          <span className="brand-mark"><ShoppingBag size={21}/></span>cartify
        </Link>
        <nav className="main-nav">
          {user?.role === "ADMIN" ? (
            <NavLink to="/admin">Admin</NavLink>
          ) : (
            <>
              <NavLink to="/" end>Shop</NavLink>
              {user && <NavLink to="/orders"><Package size={16}/> Orders</NavLink>}
            </>
          )}
        </nav>
        <div className="header-tools">
          {user?.role !== "ADMIN" && user && (
            <Link className="cart-link" to="/cart" aria-label="Shopping cart">
              <ShoppingCart size={20}/><span>Cart</span><b>{cartCount}</b>
            </Link>
          )}
          {user ? (
            <>
              <div className="user-chip"><UserRound size={17}/><span>{user.username || "Account"}</span></div>
              <button className="icon-button" onClick={signOut} title="Sign out"><LogOut size={18}/></button>
            </>
          ) : (
            <>
              <Link className="btn btn-small btn-outline" to="/login">Sign in</Link>
              <Link className="btn btn-small btn-primary" to="/register">Create account</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
