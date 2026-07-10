import { type Card } from "../../hooks/useCredit";
import { WalletCards } from "lucide-react";

interface CardItemProps {
  card: Card;
  onClick: () => void;
}

export function CardItem({ card, onClick }: CardItemProps) {
  const formattedLimit = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(card.available_limit || 0));

  // Cor de fallback elegante caso não venha do banco
  const cardColor = card.color || "#10B981";

  return (
    // Substitua APENAS a className do container principal no CardItem.tsx
    <div
      onClick={onClick}
      className="card-interactive group flex flex-col justify-between min-h-44 relative overflow-hidden p-6 rounded-3xl bg-surface transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-xl hover:shadow-black/5 cursor-pointer shadow-2xs border border-subtle/30"
    >
      {/* ✨ AURA DE BRILHO PERSONALIZADA: Efeito Neumórfico acetinado na cor exata do cartão */}
      <div
        className="absolute -right-10 -top-10 w-36 h-36 rounded-full blur-2xl transition-opacity duration-300 opacity-20 group-hover:opacity-40 pointer-events-none"
        style={{ backgroundColor: cardColor }}
      />

      {/* 1. TOPO: Ícone, Nome e Bandeira */}
      <div className="flex justify-between items-start mb-6 z-10">
        <div
          className="w-11 h-11 rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform duration-200 shadow-xs"
          style={{
            backgroundColor: `${cardColor}20`,
            color: cardColor,
          }}
        >
          <WalletCards size={22} />
        </div>
        <span
          className="text-[10px] font-extrabold uppercase tracking-[0.18em] px-3 py-1 rounded-lg backdrop-blur-xs shadow-2xs font-mono"
          style={{
            backgroundColor: `${cardColor}15`,
            color: cardColor,
            border: `1px solid ${cardColor}30`,
          }}
        >
          {card.brand}
        </span>
      </div>

      {/* 2. BASE: Nome do Cartão e Limite */}
      <div className="space-y-1.5 z-10">
        <h3 className="font-bold text-primary text-base tracking-tight truncate">
          {card.name}
        </h3>
        <p className="text-[10px] font-extrabold text-muted uppercase tracking-[0.15em]">
          Limite Disponível
        </p>
        <p className="text-xl sm:text-2xl font-extrabold text-primary tracking-tight">
          {formattedLimit}
        </p>
      </div>

      {/* Brilho Inferior da Lâmina */}
      <div
        className="absolute -left-10 -bottom-10 w-32 h-32 rounded-full blur-2xl transition-opacity duration-300 opacity-10 group-hover:opacity-25 pointer-events-none"
        style={{ backgroundColor: cardColor }}
      />
    </div>
  );
}
