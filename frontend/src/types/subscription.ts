export interface Subscription {
   id: string;
   user_id: string;
   category_id: string;
   account_id?: string | null;
   card_id?: string | null;
   title: string;
   amount: number;
   due_day: number;
   frequency: "monthly" | "yearly";
   status: "active" | "paused" | "cancelled";

   // Dados vindos dos JOINs
   category_name?: string;
   category_color?: string;
   account_name?: string;
   card_name?: string;

   created_at: string;
   updated_at: string;
}

export interface CreateSubscriptionData {
   title: string;
   amount: number;
   due_day: number;
   frequency: "monthly" | "yearly";
   category_id: string;
   account_id?: string | null;
   card_id?: string | null;
}

export interface UpdateSubscriptionStatusData {
   id: string;
   status: "active" | "paused" | "cancelled";
}
