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
      avatar_url?: string | null;
      preferences?: any;
      created_at: string;
      is_demo: boolean;
      two_factor_enabled: boolean;
      two_factor_secret?: string | null;
      token_version: number;
      password_reset_token_hash?: string | null;
      password_reset_expires_at?: string | Date | null;
      email_verified_at?: string | Date | null;

      recovery_codes?: string[] | string | null;
      last_totp_step?: number | null;
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
      competence?: string | null;
      session_id?: string;
      title: string;
      description?: string;
      observation?: string;
      amount: number;
      status: "pending" | "completed";
      type?: "entrada" | "saida" | null;
      expected_date?: string;
      completed_date?: string;
      created_at: string;
    };

    subscriptions: Subscription;
  }
}
