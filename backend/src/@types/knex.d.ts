import { Knex } from "knex";

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
   created_at: string;
   updated_at: string;
}

declare module "knex/types/tables" {
   export interface Tables {
      users: {
         id: string;
         name: string;
         email: string;
         password_hash: string;
         created_at: string;
      };
      accounts: {
         id: string;
         user_id: string;
         name: string;
         type: string;
         created_at: string;
      };
      categories: {
         id: string;
         user_id: string;
         name: string;
         type: "income" | "expense";
         color?: string;
         icon?: string;
         created_at: string;
      };
      transactions: {
         id: string;
         user_id?: string;
         account_id?: string;
         category_id?: string;
         subscription_id?: string | null;
         session_id?: string;
         title: string;
         description?: string;
         observation?: string;
         amount: number;
         status: "pending" | "completed";
         expected_date?: string;
         completed_date?: string;
         created_at: string;
      };

      subscriptions: Subscription;
   }
}
