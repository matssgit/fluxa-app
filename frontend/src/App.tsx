import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Dashboard } from "./pages/Dashboard";

// Mantém dados frescos e evita refetches desnecessários
const queryClient = new QueryClient({
   defaultOptions: {
      queries: {
         refetchOnWindowFocus: false,
         staleTime: 1000 * 60, // 1 minuto
      },
   },
});

function App() {
   return (
      <QueryClientProvider client={queryClient}>
         <Dashboard />
      </QueryClientProvider>
   );
}

export default App;
