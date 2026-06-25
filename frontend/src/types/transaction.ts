export interface Transaction {
   id: string;
   title: string;
   amount: number;

   // Adicionei o controle de status e datas (Fase 3)
   status: "pending" | "completed";
   expected_date?: string;
   completed_date?: string;

   // Adicionadas as relações com Conta e Categoria
   account_id: string;
   category_id?: string;
   account_name?: string;
   category_name?: string;

   session_id?: string; // Mantido como opcional caso ainda esteja na tabela
   created_at: string;
}

// O Summary agora recebe as entradas e saídas separadas do Back-end
export interface Summary {
   amount: number;
   income: number;
   expense: number;
}

// O input de nova transação atualizado para os campos que o Modal envia hoje
export interface NewTransactionInput {
   title: string;
   amount: number;
   account_id: string;
   category_id?: string;
   status: "pending" | "completed";
   expected_date?: string;
   completed_date?: string;
}
