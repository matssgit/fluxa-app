import { PrivacyMask } from "../../ui/PrivacyMask";
import type { Wallet } from "../../../types/wallet";
import {
  Calendar,
  CheckCircle2,
  Pause,
  Edit2,
  Trash2,
  Plus,
  Minus,
} from "lucide-react";

interface WalletCardProps {
  wallet: Wallet;
  onProgress: (wallet: Wallet, type: "deposit" | "withdraw") => void;
  onEdit: (wallet: Wallet) => void;
  onDelete: (wallet: Wallet) => void;
}

export function WalletCard({
  wallet,
  onProgress,
  onEdit,
  onDelete,
}: WalletCardProps) {
  const current = Number(wallet.current_amount) || 0;
  const target = Number(wallet.target_amount) || 0;
  const progress =
    target > 0 ? Math.min(100, Math.round((current / target) * 100)) : 0;
  const isCompleted = progress >= 100 || wallet.status === "completed";

  const formatDate = (dateStr?: string | null): string => {
    if (!dateStr) return "Sem data limite";
    const date = new Date(dateStr);
    return !isNaN(date.getTime())
      ? date.toLocaleDateString("pt-BR", { timeZone: "UTC" })
      : "Data flexível";
  };

  return (
    <div
      className={`relative overflow-hidden min-h-72 p-6 sm:p-7 flex flex-col justify-between border border-border rounded-tl-[2.5rem] rounded-br-[2.5rem] rounded-tr-xl rounded-bl-xl group transition-all duration-300 ease-out ${
        wallet.status === "paused"
          ? "opacity-60 bg-elevated/20"
          : "hover:-translate-y-1 hover:shadow-md bg-surface"
      }`}
    >
      <div className="absolute -right-16 -bottom-20 w-52 h-52 rounded-full border-[30px] border-brand/4 pointer-events-none" />
      <div>
        <div className="flex justify-between items-start gap-3">
          <div className="truncate pr-2">
            <h3 className="font-bold text-base sm:text-lg text-primary tracking-tight truncate group-hover:text-brand transition-colors">
              {wallet.title}
            </h3>
            {wallet.description && (
              <p className="text-xs text-muted truncate mt-0.5">
                {wallet.description}
              </p>
            )}
          </div>

          <div className="shrink-0 flex items-center gap-2">
            <div className="flex items-center gap-1 opacity-100 lg:opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={() => onEdit(wallet)}
                className="p-1.5 text-muted hover:text-brand hover:bg-brand/10 rounded-lg transition-colors cursor-pointer"
              >
                <Edit2 size={14} />
              </button>
              <button
                onClick={() => onDelete(wallet)}
                className="p-1.5 text-muted hover:text-danger hover:bg-danger/10 rounded-lg transition-colors cursor-pointer"
              >
                <Trash2 size={14} />
              </button>
            </div>

            {isCompleted ? (
              <span className="badge badge-success gap-1 text-[10px] uppercase tracking-wider">
                <CheckCircle2 size={12} />
                Atingida
              </span>
            ) : wallet.status === "paused" ? (
              <span className="inline-flex items-center gap-1 text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded bg-amber-500/10 text-amber-500 border border-amber-500/20">
                <Pause size={12} />
                Pausada
              </span>
            ) : (
              <span className="text-3xl sm:text-4xl font-extrabold tracking-[-0.06em] text-brand tabular-nums">
                {progress}%
              </span>
            )}
          </div>
        </div>

        <div className="relative py-7">
          <div className="flex justify-between items-baseline mb-2">
            <div>
              <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                Acumulado
              </span>
              <span className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
                <PrivacyMask amount={current} />
              </span>
            </div>
            <div className="text-right">
              <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted block">
                Objetivo
              </span>
              <span className="text-sm font-bold text-secondary">
                <PrivacyMask amount={target} />
              </span>
            </div>
          </div>

          <div className="w-full h-4 bg-elevated rounded-full overflow-hidden p-1 border border-border">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isCompleted
                  ? "bg-income"
                  : wallet.status === "paused"
                    ? "bg-amber-500"
                    : "bg-brand"
              }`}
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>

        <div className="relative pb-4 flex items-center gap-1.5 text-xs text-muted font-medium border-b border-subtle">
          <Calendar size={13} className="text-brand shrink-0" />
          <span>
            Previsão para{" "}
            <strong className="text-secondary">
              {formatDate(wallet.deadline)}
            </strong>
          </span>
        </div>
      </div>

      <div className="relative pt-3 flex items-center gap-2 mt-2">
        <button
          onClick={() => onProgress(wallet, "deposit")}
          disabled={isCompleted || wallet.status === "paused"}
          className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-brand/10 hover:bg-brand text-brand hover:text-white text-xs font-bold transition-all duration-200 shadow-2xs cursor-pointer disabled:opacity-40"
        >
          <Plus size={15} />
          <span>Adicionar</span>
        </button>

        <button
          onClick={() => onProgress(wallet, "withdraw")}
          disabled={current <= 0}
          className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-elevated hover:bg-surface text-secondary hover:text-primary border border-subtle/20 text-xs font-bold transition-all duration-200 shadow-2xs cursor-pointer disabled:opacity-40"
        >
          <Minus size={15} />
          <span>Reduzir</span>
        </button>
      </div>
    </div>
  );
}
