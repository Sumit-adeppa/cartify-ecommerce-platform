import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from "./AuthContext";
import LoginPage from "./LoginPage";
import RegistrationPage from "./RegistrationPage";
import CustomerHomePage from "./CustomerHomePage";
import CartPage from "./CartPage";
import OrderPage from "./OrderPage";
import AdminDashboard from "./AdminDashboard";
import "./index.css";

function Protected({ children, admin = false, customer = false }) {
  const { user, checking } = useAuth();
  const location = useLocation();

  if (checking) {
    return <div className="screen-center"><div className="spinner" /><p>Loading Cartify…</p></div>;
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
  }
  if (admin && user.role !== "ADMIN") return <Navigate to="/" replace />;
  if (customer && user.role === "ADMIN") return <Navigate to="/admin" replace />;
  return children;
}

function AppRoutes() {
  const { user, checking } = useAuth();
  const location = useLocation();

  if (checking) {
    return <div className="screen-center"><div className="spinner" /><p>Loading Cartify…</p></div>;
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={user
          ? <Navigate to={user.role === "ADMIN" ? "/admin" : "/"} replace />
          : <LoginPage />}
      />
      <Route
        path="/register"
        element={user
          ? <Navigate to={user.role === "ADMIN" ? "/admin" : "/"} replace />
          : <RegistrationPage />}
      />
      {/* Product catalogue is public. */}
      <Route
        path="/"
        element={user?.role === "ADMIN"
          ? <Navigate to="/admin" replace />
          : <CustomerHomePage />}
      />
      <Route path="/cart" element={<Protected customer><CartPage /></Protected>} />
      <Route path="/orders" element={<Protected customer><OrderPage /></Protected>} />
      <Route path="/admin" element={<Protected admin><AdminDashboard /></Protected>} />
      <Route path="*" element={<Navigate to={user ? (user.role === "ADMIN" ? "/admin" : "/") : "/"} replace state={location.state} />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
