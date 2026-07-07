import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "./contexts/AuthProvider";
import { Subscriptions } from "./pages/Subscriptions";
import { Dashboard } from "./pages/Dashboard/index";
import { CreditCards } from "./pages/CreditCards";
import { Settings } from "./pages/Settings";
import { Register } from "./pages/Register";
import { useAuth } from "./hooks/useAuth";
import { Login } from "./pages/Login";
import { DefaultLayout } from "./layouts/DefaultLayout";
import { Accounts } from "./pages/Accounts/index";

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
              <Route path="/cards" element={<CreditCards />} />
              <Route path="/subscriptions" element={<Subscriptions />} />
              <Route path="/accounts" element={<Accounts />} />
              <Route path="/settings" element={<Settings />} />

              {/* Rota temporária para Lançamentos não quebrar a navegação */}
              <Route path="/transactions" element={<Dashboard />} />

              {/* 3. BLINDAGEM ANTI-TELA BRANCA (Fallback 404) */}
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}
