import { useState } from "react";
import { PurchasesList } from "./PurchasesList";
import { PurchaseDetailsModal } from "./PurchaseDetailsModal";
import { X, Edit2, Trash2, WalletCards, Calendar } from "lucide-react";
import { usePurchases, type Card, type Purchase } from "../../../hooks/useCredit";

interface CardDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  card: Card | null;
  onEditClick: () => void;
  onDeleteClick: () => void;
}

function resolveCardColor(val?: string): string {
  if (!val) return "#10B981";
  if (val.startsWith("#") || val.startsWith("rgb")) return val;

  const map: Record<string, string> = {
    emerald: "#10B981",
    verde: "#10B981",
    green: "#10B981",
    orange: "#F97316",
    laranja: "#F97316",
    itau: "#F97316",
    dark: "#1E293B",
    preto: "#1E293B",
    black: "#1E293B",
    slate: "#334155",
    gold: "#F59E0B",
    ouro: "#F59E0B",
    amber: "#F59E0B",
    yellow: "#EAB308",
    purple: "#8B5CF6",
    roxo: "#8B5CF6",
    nubank: "#8B5CF6",
    indigo: "#6366F1",
    blue: "#3B82F6",
    azul: "#3B82F6",
    rose: "#F43F5E",
    pink: "#EC4899",
    red: "#EF4444",
    vermelho: "#EF4444",
    cyan: "#06B6D4",
    teal: "#14B8A6",
  };

  return map[val.toLowerCase()] || "#10B981";
}

export function CardDetailsModal({
  isOpen,
  onClose,
  card,
  onEditClick,
  onDeleteClick,
}: CardDetailsModalProps) {
  const [selectedPurchase, setSelectedPurchase] = useState<Purchase | null>(
    null,
  );
  const [isPurchaseModalOpen, setIsPurchaseModalOpen] = useState(false);

  const { data: purchases = [] } = usePurchases();

  if (!isOpen || !card) return null;

  const totalLimit = Number(card.total_limit || 0);
  const availableLimit = Number(card.available_limit || 0);
  const consumedLimit = totalLimit - availableLimit;
  const consumedPercentage =
    totalLimit > 0 ? (consumedLimit / totalLimit) * 100 : 0;

  const getProgressBarColor = (percentage: number) => {
    if (percentage > 90) return "bg-red-500 shadow-red-500/30";
    if (percentage > 75) return "bg-amber-500 shadow-amber-500/30";
    return "bg-brand shadow-brand/30";
  };

  const safePurchases = Array.isArray(purchases) ? purchases : [];
  const cardPurchases = safePurchases.filter((p) => p.card_id === card.id);
  const cardColor = resolveCardColor(card.color);

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        {/* se adapta ao conteúdo sem deixar espaços vazios gigantes */}
        <div className="bg-surface rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden border border-subtle/30 transition-all duration-300 h-auto max-h-[82vh] sm:max-h-[88vh] flex flex-col">
          {/* CABEÇALHO */}
          <div
            className="p-6 text-white relative shadow-md shrink-0 transition-colors duration-300"
            style={{ backgroundColor: cardColor }}
          >
            <button
              onClick={onClose}
              className="absolute top-6 right-6 p-2 bg-black/15 hover:bg-black/30 rounded-full transition-colors backdrop-blur-sm cursor-pointer text-white"
            >
              <X size={20} />
            </button>
            <div className="flex items-center gap-3.5 mb-1">
              <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center backdrop-blur-sm shadow-xs border border-white/30 text-white">
                <WalletCards size={24} />
              </div>
              <div>
                <h2 className="text-xl font-bold leading-tight tracking-tight text-white drop-shadow-xs">
                  {card.name}
                </h2>
                <span className="text-[10px] font-extrabold uppercase tracking-[0.2em] text-white/90 block mt-0.5 font-mono">
                  {card.brand}
                </span>
              </div>
            </div>
          </div>

          {/* CORPO DO MODAL */}
          <div className="p-5 sm:p-6 overflow-y-auto flex-1 space-y-5">
            <div className="bg-elevated/60 rounded-2xl p-5 border border-subtle/30 shadow-2xs">
              <div className="flex justify-between items-end mb-3">
                <div>
                  <p className="text-xs font-extrabold text-muted uppercase tracking-[0.15em]">
                    Limite Disponível
                  </p>
                  <p className="text-2xl font-extrabold text-primary tracking-tight mt-0.5">
                    {new Intl.NumberFormat("pt-BR", {
                      style: "currency",
                      currency: "BRL",
                    }).format(availableLimit)}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-[11px] font-extrabold text-muted uppercase tracking-widest mb-1">
                    Limite Total
                  </p>
                  <p className="text-sm font-bold text-secondary">
                    {new Intl.NumberFormat("pt-BR", {
                      style: "currency",
                      currency: "BRL",
                    }).format(totalLimit)}
                  </p>
                </div>
              </div>

              <div className="w-full h-2.5 bg-subtle/30 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 shadow-xs ${getProgressBarColor(consumedPercentage)}`}
                  style={{ width: `${Math.min(consumedPercentage, 100)}%` }}
                />
              </div>
              <div className="flex justify-between items-center mt-2">
                <span className="text-[11px] font-bold text-muted">
                  Consumido:{" "}
                  {new Intl.NumberFormat("pt-BR", {
                    style: "currency",
                    currency: "BRL",
                  }).format(consumedLimit)}
                </span>
                <span className="text-xs font-bold text-secondary">
                  {consumedPercentage.toFixed(1)}% utilizado
                </span>
              </div>
            </div>

            <div className="flex items-center justify-between p-4 bg-elevated/40 rounded-2xl border border-subtle/20">
              <div className="flex items-center gap-3 text-secondary">
                <div
                  className="w-10 h-10 rounded-xl flex items-center justify-center font-bold"
                  style={{
                    backgroundColor: `${cardColor}15`,
                    color: cardColor,
                  }}
                >
                  <Calendar size={18} />
                </div>
                <span className="font-bold text-sm">Vencimento da Fatura</span>
              </div>
              <span className="font-extrabold text-primary text-sm px-3.5 py-1.5 bg-surface rounded-xl border border-subtle/30 shadow-2xs font-mono">
                Dia {card.due_day}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <button
                onClick={onEditClick}
                className="flex items-center justify-center gap-2 px-4 py-3 bg-elevated hover:bg-subtle/40 text-primary rounded-xl text-xs sm:text-sm font-bold transition-all border border-subtle/30 cursor-pointer shadow-2xs"
              >
                <Edit2 size={16} className="text-brand" />
                <span>Editar Cartão</span>
              </button>

              <button
                onClick={onDeleteClick}
                className="flex items-center justify-center gap-2 px-4 py-3 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-xl text-xs sm:text-sm font-bold transition-all border border-red-500/20 cursor-pointer shadow-2xs"
              >
                <Trash2 size={16} />
                <span>Excluir Cartão</span>
              </button>
            </div>

            <div className="border-t border-subtle/20 pt-2" />

            {/* EMPTY STATE COMPACTO E CENTRALIZADO */}
            {cardPurchases.length === 0 ? (
              <div className="py-6 px-4 flex flex-col items-center justify-center text-center bg-elevated/30 rounded-2xl border border-dashed border-subtle/30">
                <p className="text-sm font-bold text-primary">
                  Nenhuma compra parcelada
                </p>
                <p className="text-xs text-muted mt-0.5 max-w-xs">
                  Este cartão está livre de movimentações ou faturas vinculadas
                  no momento.
                </p>
              </div>
            ) : (
              <PurchasesList
                purchases={cardPurchases}
                onPurchaseClick={(purchase) => {
                  setSelectedPurchase(purchase);
                  setIsPurchaseModalOpen(true);
                }}
              />
            )}
          </div>
        </div>
      </div>

      <PurchaseDetailsModal
        isOpen={isPurchaseModalOpen}
        onClose={() => {
          setIsPurchaseModalOpen(false);
          setSelectedPurchase(null);
        }}
        purchase={selectedPurchase}
      />
    </>
  );
}
