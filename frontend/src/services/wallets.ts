import { api } from "../api/client";
import type {
  Wallet,
  CreateWalletData,
  UpdateWalletData,
  TransferWalletData,
} from "../types/wallet";

export async function getWallets(): Promise<Wallet[]> {
  const response = await api.get<{ wallets: Wallet[] }>("/wallets");
  return response.data.wallets;
}

export async function getWalletById(id: string): Promise<Wallet> {
  const response = await api.get<{ wallet: Wallet }>(`/wallets/${id}`);
  return response.data.wallet;
}

export async function createWallet(data: CreateWalletData): Promise<void> {
  await api.post("/wallets", data);
}

export async function updateWallet({
  id,
  ...data
}: UpdateWalletData): Promise<void> {
  await api.patch(`/wallets/${id}`, data);
}

export async function deleteWallet(id: string): Promise<void> {
  await api.delete(`/wallets/${id}`);
}

// Endpoint para nutrir (depositar) ou resgatar dinheiro da Caixinha
export async function transferWallet(data: TransferWalletData): Promise<void> {
  await api.post(`/wallets/${data.wallet_id}/transfer`, data);
}
