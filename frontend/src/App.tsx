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
import { DefaultLayout } from "./layouts/DefaultLayout"; // <--- NOVO IMPORT
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
                     <Route path="/" element={<Dashboard />} />
                     <Route path="/cards" element={<CreditCards />} />
                     <Route path="/subscriptions" element={<Subscriptions />} />
                     <Route path="/settings" element={<Settings />} />

                     <Route path="/accounts" element={<Accounts />} />
                  </Route>
               </Routes>
            </BrowserRouter>
         </AuthProvider>
      </QueryClientProvider>
   );
}
