export type TransactionType = "credit" | "debit";

export interface Transaction {
   id: string;
   title: string;
   amount: number;
   type: TransactionType;
   session_id: string;
   created_at: string;
}

export interface Summary {
   amount: number;
}

export interface NewTransactionInput {
   title: string;
   amount: number;
   type: TransactionType;
}
