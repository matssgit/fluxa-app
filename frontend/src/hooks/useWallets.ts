import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import type {
  UpdateWalletData,
  UpdateWalletProgressData,
} from "../types/wallet";
import {
  getWallets,
  getWalletById,
  createWallet,
  updateWallet,
  deleteWallet,
  updateWalletProgress,
} from "../services/wallets";

export function useWallets() {
  return useQuery({
    queryKey: ["wallets"],
    queryFn: getWallets,
    staleTime: 1000 * 60 * 5,
  });
}

export function useWalletById(id: string) {
  return useQuery({
    queryKey: ["wallets", id],
    queryFn: () => getWalletById(id),
    enabled: !!id,
  });
}

export function useCreateWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createWallet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
    },
  });
}

export function useUpdateWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateWalletData) => updateWallet(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
    },
  });
}

export function useDeleteWallet() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteWallet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
    },
  });
}

export function useUpdateWalletProgress() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateWalletProgressData) => updateWalletProgress(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallets"] });
    },
  });
}
