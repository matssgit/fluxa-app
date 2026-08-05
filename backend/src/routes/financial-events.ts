import type { FastifyInstance } from "fastify";
import { checkAuth } from "../middlewares/check-auth.js";
import type { FinancialEventFilter } from "../types/financial-events.js";
import { FinancialEventsService } from "../services/financial-events.services.js";
import { FinancialEventCommandService } from "../services/financial-events.command.service.js";

export async function financialEventsRoutes(app: FastifyInstance) {
  const service = new FinancialEventsService();
  const commandService = new FinancialEventCommandService();

  app.get("/", { preHandler: [checkAuth] }, async (request, reply) => {
    const { sub: userId } = request.user;
    const queryParams = request.query as Record<string, any>;

    const filters: FinancialEventFilter = {
      page: queryParams.page ? Number(queryParams.page) : 1,
      pageSize: queryParams.pageSize ? Number(queryParams.pageSize) : 50,
      ...(queryParams.query && { query: queryParams.query }),
      ...(queryParams.status && {
        status: queryParams.status.split(",") as any,
      }),
      ...(queryParams.flow && { flow: queryParams.flow.split(",") as any }),
      ...(queryParams.type && { type: queryParams.type.split(",") as any }),
      ...(queryParams.accountIds && {
        accountIds: queryParams.accountIds.split(","),
      }),
      ...(queryParams.cardIds && { cardIds: queryParams.cardIds.split(",") }),
      ...(queryParams.categoryIds && {
        categoryIds: queryParams.categoryIds.split(","),
      }),
      ...(queryParams.minAmount && {
        minAmount: Number(queryParams.minAmount),
      }),
      ...(queryParams.maxAmount && {
        maxAmount: Number(queryParams.maxAmount),
      }),
      ...(queryParams.startDate && { startDate: queryParams.startDate }),
      ...(queryParams.endDate && { endDate: queryParams.endDate }),
      ...(queryParams.sort && { sort: queryParams.sort as any }),
    };

    const result = await service.getEvents(userId, filters);
    return reply.send(result);
  });

  app.patch("/:id/pay", { preHandler: [checkAuth] }, async (request, reply) => {
    const { sub: userId } = request.user;
    const { id: eventId } = request.params as { id: string };

    try {
      await commandService.markAsPaid(eventId, userId);
      return reply.status(204).send();
    } catch (error: any) {
      return reply.status(400).send({ message: error.message });
    }
  });
}
