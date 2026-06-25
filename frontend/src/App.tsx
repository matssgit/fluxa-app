import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "./contexts/AuthProvider";
import { Dashboard } from "./pages/Dashboard/index";
import { CreditCards } from "./pages/CreditCards";
import { Settings } from "./pages/Settings";
import { Register } from "./pages/Register";
import { useAuth } from "./hooks/useAuth";
import { Login } from "./pages/Login";

const queryClient = new QueryClient();

// Componente que protege as rotas privadas
function PrivateRoute({ children }: { children: React.ReactNode }) {
   const { isAuthenticated } = useAuth();

   if (!isAuthenticated) {
      return <Navigate to="/login" replace />;
   }

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
                  <Route
                     path="/settings"
                     element={
                        <PrivateRoute>
                           <Settings />
                        </PrivateRoute>
                     }
                  />
                  <Route path="/cards" element={<CreditCards />} />

                  {/* Rotas Privadas (Protegidas) */}
                  <Route
                     path="/"
                     element={
                        <PrivateRoute>
                           <Dashboard /> {/* 2. O Dashboard real entra aqui! */}
                        </PrivateRoute>
                     }
                  />
               </Routes>
            </BrowserRouter>
         </AuthProvider>
      </QueryClientProvider>
   );
}
