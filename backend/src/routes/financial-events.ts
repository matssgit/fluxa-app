import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import type { FinancialEventFilter } from "../types/financial-events.js";
import { FinancialEventsService } from "../services/financial-events.services.js";
import { FinancialEventCommandService } from "../services/financial-events.command.service.js";
import { InstallmentPaymentError } from "../services/installment-payments.service.js";
import { SubscriptionPaymentError } from "../services/subscription-payments.service.js";
import { z } from "zod";

const csvEnum = <T extends [string, ...string[]]>(values: T) =>
  z.preprocess(
    (value) =>
      typeof value === "string" && value.length > 0
        ? value.split(",")
        : value,
    z.array(z.enum(values)).nonempty(),
  );

const csvUuids = z.preprocess(
  (value) =>
    typeof value === "string" && value.length > 0 ? value.split(",") : value,
  z.array(z.string().uuid()).nonempty(),
);

const isoDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/)
  .refine((value) => {
    const parsed = new Date(`${value}T00:00:00.000Z`);
    return (
      !Number.isNaN(parsed.getTime()) &&
      parsed.toISOString().slice(0, 10) === value
    );
  }, "Data inválida.");

const financialEventQuerySchema = z
  .object({
    query: z.string().trim().min(1).max(200).optional(),
    status: csvEnum([
      "pending",
      "completed",
      "paid",
      "future",
      "cancelled",
    ]).optional(),
    flow: csvEnum(["income", "expense", "transfer"]).optional(),
    type: csvEnum([
      "transaction",
      "installment",
      "subscription",
      "transfer",
    ]).optional(),
    accountIds: csvUuids.optional(),
    cardIds: csvUuids.optional(),
    categoryIds: csvUuids.optional(),
    minAmount: z.coerce.number().finite().nonnegative().optional(),
    maxAmount: z.coerce.number().finite().nonnegative().optional(),
    startDate: isoDate.optional(),
    endDate: isoDate.optional(),
    sort: z
      .enum([
        "date_desc",
        "date_asc",
        "amount_desc",
        "amount_asc",
        "name_asc",
        "next_billing",
        "updated_at_desc",
      ])
      .optional(),
    page: z.coerce.number().int().min(1).default(1),
    pageSize: z.coerce.number().int().min(1).max(100).default(50),
  })
  .refine(
    ({ minAmount, maxAmount }) =>
      minAmount === undefined ||
      maxAmount === undefined ||
      minAmount <= maxAmount,
    { message: "minAmount não pode ser maior que maxAmount." },
  )
  .refine(
    ({ startDate, endDate }) =>
      startDate === undefined || endDate === undefined || startDate <= endDate,
    { message: "startDate não pode ser posterior a endDate." },
  );

export async function financialEventsRoutes(app: FastifyInstance) {
  const service = new FinancialEventsService();
  const commandService = new FinancialEventCommandService();

  app.get("/", { preHandler: [checkAuth] }, async (request, reply) => {
    const { sub: userId } = request.user;
    const filters = financialEventQuerySchema.parse(
      request.query,
    ) as unknown as FinancialEventFilter;

    const result = await service.getEvents(userId, filters);
    return reply.send(result);
  });

  app.patch("/:id/pay", { preHandler: [checkAuth] }, async (request, reply) => {
    const { sub: userId } = request.user;
    const { id: eventId } = request.params as { id: string };
    const body = z
      .object({ account_id: z.string().uuid().optional() })
      .default({})
      .parse(request.body);

    try {
      await commandService.markAsPaid(eventId, userId, body.account_id);
      return reply.status(204).send();
    } catch (error: unknown) {
      if (
        error instanceof InstallmentPaymentError ||
        error instanceof SubscriptionPaymentError
      ) {
        return reply.status(400).send({
          message: "Não foi possível concluir o pagamento deste lançamento.",
        });
      }
      if (
        error instanceof Error &&
        [
          "Lançamento financeiro não encontrado ou acesso negado.",
          "Selecione uma conta para pagar esta parcela.",
          "Tipo de evento não suportado para esta operação.",
        ].includes(error.message)
      ) {
        return reply.status(400).send({ message: error.message });
      }

      console.error("Erro ao pagar lançamento financeiro:", error);
      return reply.status(500).send({
        message: "Erro interno ao processar o lançamento financeiro.",
      });
    }
  });
}
