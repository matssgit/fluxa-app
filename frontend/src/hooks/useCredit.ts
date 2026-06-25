import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";

interface Card {
   id: string;
   name: string;
   brand: string;
   limit_amount: number;
   due_day: number;
}

// O Input exato que nosso Zod exige no Back-end
interface CreateCardInput {
   name: string;
   brand: string;
   limit_amount: number;
   due_day: number;
}

interface CreatePurchaseInput {
   card_id: string;
   category_id: string;
   title: string;
   store: string;
   observation?: string;
   total_amount: number;
   total_installments: number;
   purchase_date: string;
}

export function useCredit() {
   const queryClient = useQueryClient();

   // 1. Buscar Cartões
   const { data: cards = [], isLoading: isLoadingCards } = useQuery<Card[]>({
      queryKey: ["cards"],
      queryFn: async () => {
         const response = await api.get("/credit/cards");
         return response.data.cards;
      },
   });

   // 2. Criar Cartão
   const createCard = useMutation({
      mutationFn: async (card: CreateCardInput) => {
         await api.post("/credit/cards", card);
      },
      onSuccess: () => {
         queryClient.invalidateQueries({ queryKey: ["cards"] });
      },
   });

   // 3. Criar Compra (e gerar parcelas)
   const createPurchase = useMutation({
      mutationFn: async (purchase: CreatePurchaseInput) => {
         await api.post("/credit/purchases", purchase);
      },
      onSuccess: () => {
         // Invalida faturas e cartões para recalcular limite disponível no futuro
         queryClient.invalidateQueries({ queryKey: ["installments"] });
      },
   });

   return {
      cards,
      isLoadingCards,
      createCard: createCard.mutateAsync,
      createPurchase: createPurchase.mutateAsync,
   };
}
