import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DefaultLayout } from "./layouts/DefaultLayout";
import { AuthProvider } from "./contexts/AuthProvider";
import { Subscriptions } from "./pages/Subscriptions";
import { Dashboard } from "./pages/Dashboard/index";
import { Transactions } from "./pages/Transactions"; // ✅ 1. Import oficial adicionado aqui!
import { CreditCards } from "./pages/CreditCards";
import { Accounts } from "./pages/Accounts/index";
import { Settings } from "./pages/Settings";
import { Register } from "./pages/Register";
import { useAuth } from "./hooks/useAuth";
import { Login } from "./pages/Login";

const queryClient = new QueryClient();

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Rotas Públicas */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Rotas Privadas Agrupadas no Layout */}
            <Route
              element={
                <PrivateRoute>
                  <DefaultLayout />
                </PrivateRoute>
              }
            >
              {/* 1. Redirecionamento da raiz para o Dashboard */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              {/* 2. Rotas Principais do Menu */}
              <Route path="/transactions" element={<Transactions />} />{" "}
              {/* ✅ 2. Rota oficial no lugar certo! */}
              <Route path="/cards" element={<CreditCards />} />
              <Route path="/subscriptions" element={<Subscriptions />} />
              <Route path="/accounts" element={<Accounts />} />
              <Route path="/settings" element={<Settings />} />
              {/* 3. BLINDAGEM ANTI-TELA BRANCA (Fallback 404) */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
