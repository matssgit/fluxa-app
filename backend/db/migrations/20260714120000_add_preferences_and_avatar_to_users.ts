import { Knex } from "knex";

// Estrutura padrão exigida pela arquitetura oficial
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

export async function up(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    // Preparação para integração futura com Cloudinary / S3 / Supabase
    table.string("avatar_url").nullable();

    // JSONB para maior performance de leitura/indexação (PostgreSQL)
    // DefaultTo garante que usuários antigos e novos já nasçam com a estrutura correta
    table.jsonb("preferences").notNullable().defaultTo(DEFAULT_PREFERENCES);
  });
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.alterTable("users", (table) => {
    table.dropColumn("preferences");
    table.dropColumn("avatar_url");
  });
}
