export interface Transaction {
  id: string;
  title: string;
  amount: number;

  status: "pending" | "completed";
  expected_date?: string;
  completed_date?: string;

  account_id: string;
  category_id?: string;
  account_name?: string;
  category_name?: string;

  session_id?: string;
  created_at: string;
}

export interface Summary {
  amount: number;
  income: number;
  expense: number;
}

export interface NewTransactionInput {
  title: string;
  amount: number;
  account_id: string;
  category_id?: string;
  status: "pending" | "completed";
  expected_date?: string;
  completed_date?: string;
}
