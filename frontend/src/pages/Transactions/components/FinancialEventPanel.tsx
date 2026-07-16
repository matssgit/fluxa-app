import {
  X,
  Edit2,
  CreditCard,
  Calendar,
  Repeat,
  ShoppingBag,
  CheckCircle2,
  Clock,
  Check,
} from "lucide-react";
import { PrivacyMask } from "../../../components/ui/PrivacyMask";
import type { FinancialEventDTO } from "../types";

interface FinancialEventPanelProps {
  event: FinancialEventDTO | null;
  isOpen: boolean;
  onClose: () => void;
  onEdit: () => void;
  onMarkAsPaid: (eventId: string) => void;
}

export function FinancialEventPanel({
  event,
  isOpen,
  onClose,
  onEdit,
  onMarkAsPaid,
}: FinancialEventPanelProps) {
  if (!isOpen || !event) return null;

  const formatDate = (dateString?: string) => {
    if (!dateString) return "Sem data";
    return new Intl.DateTimeFormat("pt-BR", {
      day: "2-digit",
      month: "long",
      year: "numeric",
    }).format(new Date(dateString));
  };

  const isIncome = event.flow === "income";
  const colorClass = isIncome ? "text-emerald-500" : "text-red-500";

  // ============================================================================
  // RENDER: PARCELAMENTOS
  // ============================================================================
  const renderInstallmentContext = () => {
    const ctx = event.context;
    if (!ctx || !ctx.installmentNumber || !ctx.totalInstallments) return null;

    const totalPaid = Math.abs(event.amount) * (ctx.installmentNumber - 1);
    const totalRemaining =
      Math.abs(event.amount) *
      (ctx.totalInstallments - ctx.installmentNumber + 1);
    const totalAmount = Math.abs(event.amount) * ctx.totalInstallments;
    const progressPercent = Math.round(
      (ctx.installmentNumber / ctx.totalInstallments) * 100,
    );

    return (
      <div className="space-y-5 sm:space-y-6 animate-fade-in">
        <div className="p-4 sm:p-5 rounded-2xl bg-elevated/30 border border-subtle/20 space-y-4">
          <div className="flex justify-between items-end">
            <div>
              <p className="text-[10px] sm:text-[11px] font-extrabold uppercase tracking-wider text-muted mb-1">
                Progresso
              </p>
              <p className="text-base sm:text-lg font-bold text-primary">
                {progressPercent}% Pago
              </p>
            </div>
            <div className="text-right">
              <p className="text-[10px] sm:text-[11px] font-extrabold uppercase tracking-wider text-muted mb-1">
                Valor Total
              </p>
              <p className="text-xs sm:text-sm font-bold text-secondary">
                R$ {totalAmount.toFixed(2)}
              </p>
            </div>
          </div>

          <div className="w-full h-2 sm:h-2.5 bg-subtle/20 rounded-full overflow-hidden">
            <div
              className="h-full bg-brand rounded-full transition-all"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <div className="flex justify-between text-[11px] sm:text-xs font-medium">
            <span className="text-emerald-500">
              Pagos: R$ {totalPaid.toFixed(2)}
            </span>
            <span className="text-muted">
              Restam: R$ {totalRemaining.toFixed(2)}
            </span>
          </div>
        </div>

        <div className="pt-2">
          <h3 className="text-[10px] sm:text-xs font-extrabold tracking-widest text-muted uppercase mb-4 pl-1">
            Linha do Tempo
          </h3>
          <div className="relative pl-3 border-l-2 border-subtle/20 space-y-5 sm:space-y-6 ml-2">
            {/* Parcela Anterior */}
            {ctx.installmentNumber > 1 && (
              <div className="relative">
                <div className="absolute -left-4.25 top-1 w-3 h-3 rounded-full bg-emerald-500 ring-4 ring-surface" />
                <div className="pl-4">
                  <p className="text-xs sm:text-sm font-bold text-secondary">
                    Parcela {ctx.installmentNumber - 1}/{ctx.totalInstallments}
                  </p>
                  <p className="text-[10px] sm:text-xs font-medium text-emerald-500 flex items-center gap-1 mt-0.5">
                    <Check size={12} /> Paga
                  </p>
                </div>
              </div>
            )}

            {/* Parcela Atual */}
            <div className="relative">
              <div className="absolute -left-5.25 top-0 w-5 h-5 rounded-full bg-brand ring-4 ring-surface flex items-center justify-center">
                <div className="w-2 h-2 rounded-full bg-white" />
              </div>
              <div className="pl-4">
                <p className="text-xs sm:text-sm font-bold text-primary">
                  Parcela {ctx.installmentNumber}/{ctx.totalInstallments}
                </p>
                <div className="flex flex-wrap items-center gap-2 mt-1">
                  <span
                    className={`text-[11px] sm:text-xs font-bold ${colorClass}`}
                  >
                    <PrivacyMask amount={event.amount} />
                  </span>
                  <span className="text-[10px] sm:text-xs font-medium text-muted px-2 py-0.5 rounded-md bg-elevated border border-subtle/20">
                    Mês atual
                  </span>
                </div>
              </div>
            </div>

            {/* Parcela Futura */}
            {ctx.installmentNumber < ctx.totalInstallments && (
              <div className="relative">
                <div className="absolute -left-4.25 top-1 w-3 h-3 rounded-full bg-subtle/30 ring-4 ring-surface" />
                <div className="pl-4 opacity-60">
                  <p className="text-xs sm:text-sm font-bold text-secondary">
                    Parcela {ctx.installmentNumber + 1}/{ctx.totalInstallments}
                  </p>
                  <p className="text-[10px] sm:text-xs font-medium text-muted mt-0.5">
                    Próximo mês
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  // ============================================================================
  // RENDER: ASSINATURAS E RECORRÊNCIAS
  // ============================================================================
  const renderSubscriptionContext = () => {
    const ctx = event.context;
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center gap-4 p-4 sm:p-5 rounded-2xl bg-brand/5 border border-brand/10">
          <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl bg-brand/10 text-brand flex items-center justify-center shrink-0">
            <Repeat size={20} className="sm:w-6 sm:h-6" />
          </div>
          <div>
            <p className="text-[10px] sm:text-[11px] font-extrabold uppercase tracking-wider text-brand mb-1">
              Próxima Cobrança
            </p>
            <p className="text-xs sm:text-sm font-bold text-primary">
              {formatDate(ctx?.nextBillingDate || event.date)}
            </p>
          </div>
        </div>
      </div>
    );
  };

  // ============================================================================
  // RENDER: TRANSAÇÃO SIMPLES À VISTA
  // ============================================================================
  const renderTransactionContext = () => {
    return (
      <div className="space-y-4 animate-fade-in">
        <div className="p-5 rounded-2xl bg-elevated/30 border border-subtle/20 flex flex-col items-center justify-center text-center">
          <ShoppingBag size={24} className="text-muted mb-3" />
          <p className="text-xs sm:text-sm font-bold text-secondary">
            Lançamento Único
          </p>
          <p className="text-[11px] sm:text-xs font-medium text-muted mt-1 max-w-62.5">
            Esta movimentação não possui vínculos com parcelamentos ou
            assinaturas ativas.
          </p>
        </div>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-0 sm:justify-end">
      {/* Overlay Escuro */}
      <div
        className="absolute inset-0 bg-surface/80 backdrop-blur-sm animate-fade-in"
        onClick={onClose}
      />

      {/* ✨ O SEGREDO ESTÁ AQUI: max-h-[80dvh]
          Isto garante que o modal no Mobile NUNCA ultrapassa 80% da altura da tela, 
          deixando 10% de folga em cima (Header) e 10% em baixo (Sidebar). 
          No Desktop (sm:), ele volta a assumir h-dvh (100%) como um painel lateral clássico. 
      */}
      <div className="relative w-full max-w-md sm:max-w-none sm:w-120 max-h-[80dvh] sm:max-h-none sm:h-dvh bg-surface rounded-3xl sm:rounded-none border border-subtle/20 sm:border-0 sm:border-l shadow-2xl flex flex-col animate-scale-in sm:animate-slide-in-right overflow-hidden">
        {/* TOP BAR */}
        <div className="flex items-center justify-between px-5 py-3 sm:px-6 sm:py-4 border-b border-subtle/20 bg-elevated/30 shrink-0">
          <button
            onClick={onClose}
            className="p-2 text-muted hover:text-primary hover:bg-surface rounded-full transition-colors cursor-pointer border border-transparent hover:border-subtle/30"
          >
            <X size={18} className="sm:w-5 sm:h-5" />
          </button>

          <button
            onClick={onEdit}
            className="flex items-center gap-2 px-3 py-1.5 sm:px-4 sm:py-2 rounded-xl bg-surface border border-subtle/30 text-[11px] sm:text-sm font-bold text-secondary hover:text-primary hover:border-subtle/50 transition-colors cursor-pointer shadow-sm"
          >
            <Edit2 size={12} className="sm:w-3.5 sm:h-3.5" />
            <span>Editar Evento</span>
          </button>
        </div>

        {/* ✨ ÁREA COM ROLAGEM INTERNA (overflow-y-auto) 
            Se o conteúdo (timeline de 24 parcelas, por exemplo) for maior que os 80% de limite, 
            é AQUI dentro que vai scrollar, mantendo o modal imóvel e dentro dos limites. */}
        <div className="flex-1 overflow-y-auto scrollbar-thin flex flex-col">
          {/* HEADER RESUMO */}
          <div className="px-5 pt-6 pb-5 sm:px-8 sm:pt-8 sm:pb-6 border-b border-subtle/10 flex flex-col items-center text-center shrink-0">
            <div className="w-10 h-10 sm:w-14 sm:h-14 rounded-2xl bg-elevated flex items-center justify-center text-primary border border-subtle/20 mb-3 sm:mb-4 shadow-sm">
              {event.type === "installment" && (
                <CreditCard size={20} className="sm:w-6 sm:h-6" />
              )}
              {event.type === "subscription" && (
                <Repeat size={20} className="sm:w-6 sm:h-6" />
              )}
              {event.type === "transaction" && (
                <ShoppingBag size={20} className="sm:w-6 sm:h-6" />
              )}
            </div>

            <h2 className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight mb-1 sm:mb-2 line-clamp-2">
              {event.title}
            </h2>

            <div
              className={`text-2xl sm:text-3xl font-extrabold tracking-tight mb-4 ${colorClass}`}
            >
              <PrivacyMask amount={event.amount} />
            </div>

            <div className="flex flex-wrap justify-center gap-2">
              <span className="flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-elevated border border-subtle/20 text-[10px] sm:text-xs font-bold text-secondary">
                <Calendar size={10} className="sm:w-3 sm:h-3 text-muted" />
                {formatDate(event.date)}
              </span>
              <span className="flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-elevated border border-subtle/20 text-[10px] sm:text-xs font-bold text-secondary">
                <CreditCard size={10} className="sm:w-3 sm:h-3 text-muted" />
                {event.account || "Sem conta"}
              </span>

              {event.status === "pending" ? (
                <span className="flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-amber-500/10 border border-amber-500/20 text-[10px] sm:text-xs font-bold text-amber-600">
                  <Clock size={10} className="sm:w-3 sm:h-3" /> Pendente
                </span>
              ) : (
                <span className="flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[10px] sm:text-xs font-bold text-emerald-600">
                  <CheckCircle2 size={10} className="sm:w-3 sm:h-3" /> Concluído
                </span>
              )}
            </div>
          </div>

          {/* BOTÃO DAR BAIXA */}
          {event.status === "pending" && (
            <div className="px-5 py-4 sm:px-8 sm:py-5 border-b border-subtle/10 bg-surface shrink-0">
              <button
                onClick={() => onMarkAsPaid(event.id)}
                className="w-full flex items-center justify-center gap-2 py-3 sm:py-3.5 rounded-2xl bg-emerald-500 text-white text-xs sm:text-sm font-bold shadow-md hover:bg-emerald-600 transition-colors active:scale-95"
              >
                <CheckCircle2 size={18} className="sm:w-5 sm:h-5" />
                Dar baixa no pagamento
              </button>
            </div>
          )}

          {/* BODY: CONTEXTO */}
          <div className="p-5 sm:p-8 bg-surface shrink-0">
            {event.type === "installment" && renderInstallmentContext()}
            {event.type === "subscription" && renderSubscriptionContext()}
            {event.type === "transaction" && renderTransactionContext()}
          </div>
        </div>
      </div>
    </div>
  );
}
