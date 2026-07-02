export interface TransactionRow {
   id: string;
   title: string;
   description: string;
   amount: string | number;
   date: string;
   type: "entrada" | "saida";
}

export interface PurchaseRow {
   id: string;
   title: string;
   description: string;
   amount: string | number;
   date: string;
}

export interface PendingTransaction {
   id: string;
   title: string;
   amount: string | number;
   expected_date: string;
}

export interface PendingInstallment {
   id: string;
   installment_number: number;
   total_installments: number;
   title: string;
   card_name: string;
   amount: string | number;
   expected_date: string;
}
