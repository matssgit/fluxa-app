import { useState } from "react";
import { X, Edit2, Trash2, CreditCard, Calendar } from "lucide-react";
import { usePurchases, type Card, type Purchase } from "../../hooks/useCredit";
import { getCardGradient } from "../../utils/cardColors";
import { PurchasesList } from "./PurchasesList";
import { PurchaseDetailsModal } from "./PurchaseDetailsModal";

interface CardDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  card: Card | null;
  onEditClick: () => void;
  onDeleteClick: () => void;
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

  // Atualizado internamente ou aplicado via classe do utilitário com a nova sintaxe
  const gradientClass = getCardGradient(card.color);

  const totalLimit = Number(card.total_limit || 0);
  const availableLimit = Number(card.available_limit || 0);
  const consumedLimit = totalLimit - availableLimit;
  const consumedPercentage =
    totalLimit > 0 ? (consumedLimit / totalLimit) * 100 : 0;

  // Lógica inteligente para cor da barra de limite consumido
  const getProgressBarColor = (percentage: number) => {
    if (percentage > 90) return "bg-red-500 shadow-red-500/30";
    if (percentage > 75) return "bg-amber-500 shadow-amber-500/30";
    return "bg-brand shadow-brand/30";
  };

  // Filtro Seguro
  const safePurchases = Array.isArray(purchases) ? purchases : [];
  const cardPurchases = safePurchases.filter((p) => p.card_id === card.id);

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl w-full max-w-lg shadow-2xl overflow-hidden border border-subtle/30 transition-all duration-300">
          {/* 1. CABEÇALHO COM GRADIENTE V4 (bg-linear-to-tr) */}
          <div
            className={`p-6 bg-linear-to-tr ${gradientClass} text-white relative shadow-sm`}
          >
            <button
              onClick={onClose}
              className="absolute top-6 right-6 p-2 bg-white/10 hover:bg-white/20 rounded-full transition-colors backdrop-blur-sm cursor-pointer"
            >
              <X size={20} />
            </button>
            <div className="flex items-center gap-3.5 mb-1">
              <div className="w-11 h-11 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur-sm shadow-xs border border-white/30">
                <CreditCard size={22} />
              </div>
              <div>
                <h2 className="text-xl font-bold leading-tight tracking-tight">
                  {card.name}
                </h2>
                <span className="text-[10px] font-extrabold uppercase tracking-[0.2em] text-white/80 block mt-0.5">
                  {card.brand}
                </span>
              </div>
            </div>
          </div>

          {/* 2. CORPO DO MODAL (Escala nativa e rolagem suave) */}
          <div className="p-6 overflow-y-auto max-h-[65vh] space-y-6">
            {/* Resumo do Limite Neumórfico */}
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

              {/* Barra de Progresso Inteligente */}
              <div className="w-full h-2.5 bg-subtle/30 rounded-full overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full transition-all duration-500 shadow-xs ${getProgressBarColor(
                    consumedPercentage,
                  )}`}
                  style={{
                    width: `${Math.min(consumedPercentage, 100)}%`,
                  }}
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

            {/* Informação do Vencimento da Fatura */}
            <div className="flex items-center justify-between p-4 bg-elevated/40 rounded-2xl border border-subtle/20">
              <div className="flex items-center gap-3 text-secondary">
                <div className="w-9 h-9 rounded-xl bg-brand/10 text-brand flex items-center justify-center">
                  <Calendar size={18} />
                </div>
                <span className="font-bold text-sm">Vencimento da Fatura</span>
              </div>
              <span className="font-extrabold text-primary text-sm px-3 py-1 bg-surface rounded-lg border border-subtle/30 shadow-2xs">
                Dia {card.due_day}
              </span>
            </div>

            {/* Ações Rápidas de Edição e Exclusão */}
            <div className="grid grid-cols-2 gap-3 pt-1">
              <button
                onClick={onEditClick}
                className="flex items-center justify-center gap-2 px-4 py-3 bg-elevated hover:bg-subtle/40 text-primary rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 border border-subtle/30 cursor-pointer shadow-2xs"
              >
                <Edit2 size={16} className="text-brand" />
                <span>Editar Cartão</span>
              </button>

              <button
                onClick={onDeleteClick}
                className="flex items-center justify-center gap-2 px-4 py-3 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 border border-red-500/20 cursor-pointer shadow-2xs"
              >
                <Trash2 size={16} />
                <span>Excluir Cartão</span>
              </button>
            </div>

            <div className="border-t border-subtle/20 pt-2" />

            {/* Lista de Compras Conectada */}
            <PurchasesList
              purchases={cardPurchases}
              onPurchaseClick={(purchase) => {
                setSelectedPurchase(purchase);
                setIsPurchaseModalOpen(true);
              }}
            />
          </div>
        </div>
      </div>

      {/* Modal de Detalhes da Compra */}
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
