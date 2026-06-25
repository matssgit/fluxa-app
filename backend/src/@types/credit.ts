export interface Card {
   id: string;
   user_id: string;
   name: string;
   brand: string;
   limit_amount: number;
   due_day: number;
   created_at: string;
}

export interface CreditPurchase {
   id: string;
   user_id: string;
   card_id: string;
   category_id: string;
   title: string;
   store: string;
   observation?: string;
   total_amount: number;
   total_installments: number;
   purchase_date: string;
   created_at: string;
}

export interface Installment {
   id: string;
   user_id: string;
   purchase_id: string;
   installment_number: number;
   total_installments: number;
   amount: number;
   expected_date: string;
   completed_date?: string;
   status: "pending" | "paid";
}
