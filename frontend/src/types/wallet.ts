export interface Wallet {
  id: string;
  user_id: string;
  title: string;
  description?: string;
  target_amount: number;
  current_amount: number;
  deadline?: string | null;
  color?: string;
  status: "active" | "completed" | "paused";
  created_at: string;
  updated_at: string;
}

export interface CreateWalletData {
  title: string;
  description?: string;
  target_amount: number;
  current_amount?: number;
  deadline?: string | null;
  color?: string;
}

export interface UpdateWalletData {
  id: string;
  title?: string;
  description?: string;
  target_amount?: number;
  deadline?: string | null;
  status?: "active" | "completed" | "paused";
  color?: string;
}

export interface UpdateWalletProgressData {
  wallet_id: string;
  type: "deposit" | "withdraw";
  amount: number;
  observation?: string;
}
