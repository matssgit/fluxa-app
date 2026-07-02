import { api } from "../api/client";
import type {
   Subscription,
   CreateSubscriptionData,
   UpdateSubscriptionStatusData,
} from "../types/subscription";

export async function getSubscriptions(): Promise<Subscription[]> {
   const response = await api.get<{ subscriptions: Subscription[] }>(
      "/subscriptions",
   );
   return response.data.subscriptions;
}

export async function createSubscription(
   data: CreateSubscriptionData,
): Promise<void> {
   await api.post("/subscriptions", data);
}

export async function updateSubscriptionStatus({
   id,
   status,
}: UpdateSubscriptionStatusData): Promise<void> {
   await api.patch(`/subscriptions/${id}/status`, { status });
}

export async function deleteSubscription(id: string): Promise<void> {
   await api.delete(`/subscriptions/${id}`);
}

export async function paySubscription(
   id: string,
   account_id: string,
): Promise<void> {
   await api.post(`/subscriptions/${id}/pay`, { account_id });
}
