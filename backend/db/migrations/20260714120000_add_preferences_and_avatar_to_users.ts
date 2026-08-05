import { Knex } from "knex";

const DEFAULT_PREFERENCES = JSON.stringify({
  theme: "system",
  privacy: {
    hide_balance: false,
  },
  notifications: {
    reminders_enabled: true,
    subscriptions_enabled: true,
  },
});

export async function up(knex: Knex): Promise<void> {}

export async function down(knex: Knex): Promise<void> {}
