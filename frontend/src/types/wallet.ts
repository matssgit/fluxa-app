export interface Wallet {
  id: string;
  user_id: string;
  title: string;
  description?: string;
  target_amount: number; // Meta total de crescimento (Colheita)
  current_amount: number; // Acumulado atual (O quanto já cresceu)
  deadline?: string | null; // Data limite estimada (ISO String ou YYYY-MM-DD)
  color?: string; // Token visual para tag (ex: 'brand', 'accent', 'emerald')
  status: "active" | "completed" | "paused";
  created_at: string;
  updated_at: string;
}

export interface CreateWalletData {
  title: string;
  description?: string;
  target_amount: number;
  current_amount?: number; // Pode começar em 0 ou já com um aporte inicial
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
