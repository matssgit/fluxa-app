import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getWallets,
  getWalletById,
  createWallet,
  updateWallet,
  deleteWallet,
  transferWallet,
} from "../services/wallets";
import type { UpdateWalletData, TransferWalletData } from "../types/wallet";

// Hook para buscar todas as Caixinhas/Metas
export function useWallets() {
  return useQuery({
    queryKey: ["wallets"],
    queryFn: getWallets,
    staleTime: 1000 * 60 * 5, // 5 minutos de cache
  });
}

// Hook para buscar detalhes de uma meta específica
export function useWalletById(id: string) {
  return useQuery({
    queryKey: ["wallets", id],
    queryFn: () => getWalletById(id),
    enabled: !!id,
  });
}

// Hook para criar uma nova meta no ecossistema
export function useCreateWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createWallet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para atualizar dados ou pausar/concluir meta
export function useUpdateWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateWalletData) => updateWallet(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para podar (excluir) meta do sistema
export function useDeleteWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteWallet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}

// Hook para Aportes (Nutrir) ou Resgates (Colher)
export function useTransferWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: TransferWalletData) => transferWallet(data),
    onSuccess: () => {
      // Invalidação em cascata completa de liquidez e patrimônio
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
      queryClient.invalidateQueries({ queryKey: ["accounts"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
