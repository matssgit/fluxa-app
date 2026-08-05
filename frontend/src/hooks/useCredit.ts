import { api } from "../api/client";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

// --- INTERFACES ---
export interface Card {
  id: string;
  name: string;
  brand: string;
  total_limit: number;
  available_limit: number;
  due_day: number;
  color?: string;
}

export interface CreateCardInput {
  name: string;
  brand: string;
  limit_amount: number;
  due_day: number;
  color?: string;
}

export interface CreatePurchaseInput {
  card_id: string;
  category_id: string;
  title: string;
  store: string;
  observation?: string;
  total_amount: number;
  total_installments: number;
  purchase_date: string;
}

export interface EditCardInput {
  id: string;
  name: string;
  brand: string;
  total_limit: number;
  due_day: number;
  color?: string;
}

export interface Installment {
  id: string;
  purchase_id: string;
  installment_number: number;
  total_installments: number;
  amount: number;
  expected_date: string;
  status: "pending" | "paid" | "cancelled";
  completed_date?: string | null;
  purchase_title?: string;
}

export interface Purchase {
  id: string;
  card_id: string;
  category_id: string;
  title: string;
  store: string;
  total_amount: number;
  total_installments: number;
  purchase_date: string;
  status: "active" | "cancelled";
}

// --- QUERIES (Busca de Dados) ---

export const useCards = () => {
  return useQuery<Card[]>({
    queryKey: ["cards"],
    queryFn: async () => {
      const response = await api.get("/credit/cards");
      return response.data.cards || [];
    },
  });
};

export const useInstallments = (purchaseId?: string) => {
  return useQuery<Installment[]>({
    queryKey: ["installments", purchaseId],
    queryFn: async () => {
      const url = purchaseId
        ? `/credit/installments?purchase_id=${purchaseId}`
        : "/credit/installments";
      const response = await api.get(url);
      return response.data.installments || [];
    },
    enabled: !!purchaseId,
  });
};

export const usePurchases = () => {
  return useQuery<Purchase[]>({
    queryKey: ["purchases"],
    queryFn: async () => {
      const response = await api.get("/credit/purchases");
      return response.data.purchases || [];
    },
  });
};

// --- MUTATIONS (Alteração de Dados) ---

export const useCreateCard = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (card: CreateCardInput) => api.post("/credit/cards", card),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cards"] }),
  });
};

export const useCreatePurchase = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (purchase: CreatePurchaseInput) =>
      api.post("/credit/purchases", purchase),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["installments"] });
      queryClient.invalidateQueries({ queryKey: ["cards"] });
      queryClient.invalidateQueries({ queryKey: ["purchases"] });
    },
  });
};

export const useEditCard = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (card: EditCardInput) =>
      api.put(`/credit/cards/${card.id}`, card),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cards"] }),
  });
};

export const usePayInstallment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      installmentId,
      accountId,
    }: {
      installmentId: string;
      accountId: string;
    }) => {
      await api.post(`/credit/installments/${installmentId}/pay`, {
        account_id: accountId,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cards"] });
      queryClient.invalidateQueries({ queryKey: ["purchases"] });
      queryClient.invalidateQueries({ queryKey: ["installments"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
    },
  });
};

export const useCancelPurchase = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.patch(`/credit/purchases/${id}/cancel`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["installments"] });
      queryClient.invalidateQueries({ queryKey: ["purchases"] });
      queryClient.invalidateQueries({ queryKey: ["cards"] });
    },
  });
};

export const useDeleteCard = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete(`/credit/cards/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["cards"] });
      queryClient.invalidateQueries({ queryKey: ["purchases"] });
      queryClient.invalidateQueries({ queryKey: ["installments"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
};
