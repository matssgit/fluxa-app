import { Knex } from "knex";

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
   }
}
