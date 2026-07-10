import {
  Repeat,
  Tag,
  Building,
  CreditCard,
  Calendar,
  Play,
  Pause,
  Ban,
  Trash2,
  CheckCircle2,
} from "lucide-react";
import { type Subscription } from "../../types/subscription";
import {
  useUpdateSubscriptionStatus,
  useDeleteSubscription,
} from "../../hooks/useSubscriptions";

interface SubscriptionCardProps {
  subscription: Subscription;
  onPay: (id: string) => void;
}

export function SubscriptionCard({
  subscription,
  onPay,
}: SubscriptionCardProps) {
  const { mutate: updateStatus, isPending: isUpdating } =
    useUpdateSubscriptionStatus();
  const { mutate: deleteSub, isPending: isDeleting } = useDeleteSubscription();

  const isCardPayment = !!subscription.card_id || !!subscription.card_name;
  const sourceName =
    subscription.card_name || subscription.account_name || "Cobrança Padrão";
  const categoryName = subscription.category_name || "Geral";
  const status = subscription.status || "active";

  const formatCurrency = (val: number): string =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(isNaN(val) ? 0 : val);

  function handleStatusChange(
    newStatus: "active" | "paused" | "cancelled",
  ): void {
    updateStatus({ id: subscription.id, status: newStatus });
  }

  function handleDelete(): void {
    if (
      window.confirm(
        "Deseja realmente podar este serviço recorrente do seu histórico?",
      )
    ) {
      deleteSub(subscription.id);
    }
  }

  return (
    <div
      className={`card-default p-6 flex flex-col justify-between border-subtle/30 group transition-all duration-300 ease-out ${
        status === "cancelled"
          ? "opacity-60 bg-elevated/20"
          : "hover:-translate-y-1 hover:shadow-xl hover:shadow-black/5 bg-surface"
      }`}
    >
      {/* Topo: Título + Badge de Status */}
      <div>
        <div className="flex justify-between items-start gap-3">
          <div className="truncate">
            <h3 className="font-bold text-base sm:text-lg text-primary tracking-tight truncate group-hover:text-brand transition-colors">
              {subscription.title}
            </h3>
            <div className="flex items-center gap-2 mt-1">
              <span className="inline-block text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded bg-elevated text-secondary border border-subtle/20">
                {subscription.frequency === "yearly" ? "Anual" : "Mensal"}
              </span>

              {/* Badges Semânticos de Status */}
              {status === "active" && (
                <span className="inline-flex items-center gap-1 text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded bg-brand/10 text-brand border border-brand/20">
                  <span className="w-1.5 h-1.5 rounded-full bg-brand animate-pulse" />
                  Ativa
                </span>
              )}
              {status === "paused" && (
                <span className="inline-flex items-center gap-1 text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded bg-amber-500/10 text-amber-500 border border-amber-500/20">
                  Pausada
                </span>
              )}
              {status === "cancelled" && (
                <span className="inline-flex items-center gap-1 text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded bg-elevated text-muted border border-subtle/30">
                  Cancelada
                </span>
              )}
            </div>
          </div>

          <div className="w-10 h-10 rounded-2xl bg-elevated flex items-center justify-center text-muted group-hover:bg-brand/10 group-hover:text-brand transition-colors shrink-0">
            <Repeat size={18} />
          </div>
        </div>

        {/* Centro: Valor em Evidência */}
        <div className="py-4">
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
            Valor Recorrente
          </span>
          <p className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight mt-0.5">
            {formatCurrency(Number(subscription.amount) || 0)}
          </p>
        </div>

        {/* Categoria e Fonte */}
        <div className="pb-3 flex items-center justify-between text-xs text-muted font-medium border-b border-subtle/20">
          <div className="flex items-center gap-2 truncate">
            <div
              className="flex items-center gap-1 truncate"
              title={`Categoria: ${categoryName}`}
            >
              <Tag size={13} className="text-brand shrink-0" />
              <span className="truncate">{categoryName}</span>
            </div>
            <span className="text-subtle/40">•</span>
            <div
              className="flex items-center gap-1 truncate"
              title={`Fonte: ${sourceName}`}
            >
              {isCardPayment ? (
                <CreditCard size={13} className="text-brand shrink-0" />
              ) : (
                <Building size={13} className="text-brand shrink-0" />
              )}
              <span className="truncate">{sourceName}</span>
            </div>
          </div>

          <div className="flex items-center gap-1 shrink-0 ml-2 text-secondary font-bold bg-surface px-2 py-1 rounded-lg border border-subtle/30 shadow-2xs">
            <Calendar size={12} className="text-brand" />
            <span>Dia {subscription.due_day}</span>
          </div>
        </div>
      </div>

      {/* Rodapé: Ações Operacionais e Ciclo de Vida */}
      <div className="pt-3 flex items-center justify-between gap-2 mt-2">
        {/* Botão de Baixa (Só aparece se estiver ativa) */}
        {status === "active" ? (
          <button
            onClick={() => onPay(subscription.id)}
            disabled={isUpdating || isDeleting}
            className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-brand/10 hover:bg-brand text-brand hover:text-white text-xs font-bold transition-all duration-200 shadow-2xs cursor-pointer disabled:opacity-50"
          >
            <CheckCircle2 size={15} />
            <span>Pagar Mês</span>
          </button>
        ) : (
          <div className="flex-1 text-[11px] font-semibold text-muted italic flex items-center gap-1">
            <span>
              {status === "paused"
                ? "Cobrança suspensa temporariamente"
                : "Serviço encerrado"}
            </span>
          </div>
        )}

        {/* Controles Rápidos de Status (Ícones) */}
        <div className="flex items-center gap-1 shrink-0 bg-elevated/60 p-1 rounded-xl border border-subtle/20">
          {status === "active" && (
            <button
              onClick={() => handleStatusChange("paused")}
              disabled={isUpdating}
              title="Pausar cobrança"
              className="p-1.5 text-muted hover:text-amber-500 hover:bg-surface rounded-lg transition-all cursor-pointer disabled:opacity-50"
            >
              <Pause size={15} />
            </button>
          )}

          {status === "paused" && (
            <button
              onClick={() => handleStatusChange("active")}
              disabled={isUpdating}
              title="Reativar serviço"
              className="p-1.5 text-muted hover:text-brand hover:bg-surface rounded-lg transition-all cursor-pointer disabled:opacity-50"
            >
              <Play size={15} />
            </button>
          )}

          {status !== "cancelled" && (
            <button
              onClick={() => handleStatusChange("cancelled")}
              disabled={isUpdating}
              title="Cancelar assinatura"
              className="p-1.5 text-muted hover:text-danger hover:bg-surface rounded-lg transition-all cursor-pointer disabled:opacity-50"
            >
              <Ban size={15} />
            </button>
          )}

          {status === "cancelled" && (
            <button
              onClick={handleDelete}
              disabled={isDeleting}
              title="Excluir do histórico"
              className="p-1.5 text-muted hover:text-danger hover:bg-surface rounded-lg transition-all cursor-pointer disabled:opacity-50"
            >
              <Trash2 size={15} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
