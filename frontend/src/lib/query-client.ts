import { QueryClient } from "@tanstack/react-query";
import { AvatarStorage } from "./storage/AvatarStorage";

export const queryClient = new QueryClient();
export const AUTH_SESSION_CLEARED_EVENT = "fluxa:auth-session-cleared";

export async function clearClientSession(): Promise<void> {
  localStorage.removeItem("@FinanceApp:token");
  localStorage.removeItem("@FinanceApp:user");
  AvatarStorage.clear();

  await queryClient.cancelQueries();
  queryClient.clear();
}
