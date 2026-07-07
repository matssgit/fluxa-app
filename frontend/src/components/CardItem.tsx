import { type Card } from "../hooks/useCredit";
import { CreditCard } from "lucide-react";

interface CardItemProps {
  card: Card;
  onClick: () => void;
}

export function CardItem({ card, onClick }: CardItemProps) {
  // Formatador de moeda nativo
  const formattedLimit = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(card.available_limit || 0));

  return (
    <div
      onClick={onClick}
      className="card-interactive group flex flex-col justify-between min-h-40 relative overflow-hidden"
    >
      {/* 1. TOPO: Ícone e Bandeira */}
      <div className="flex justify-between items-start mb-6 z-10">
        <div className="w-10 h-10 rounded-xl bg-brand/10 border border-brand/15 flex items-center justify-center text-brand group-hover:scale-110 transition-transform duration-200 shadow-xs">
          <CreditCard size={20} />
        </div>
        <span className="text-[10px] font-extrabold uppercase tracking-[0.18em] text-secondary border border-subtle/40 bg-surface/60 backdrop-blur-xs px-2.5 py-1 rounded-md shadow-2xs">
          {card.brand}
        </span>
      </div>

      {/* 2. BASE: Nome do Cartão e Limite */}
      <div className="space-y-1 z-10">
        <h3 className="font-bold text-primary text-base tracking-tight truncate">
          {card.name}
        </h3>
        <p className="text-[10px] font-extrabold text-muted uppercase tracking-[0.15em]">
          Limite Disponível
        </p>
        <p className="text-xl font-extrabold text-primary tracking-tight">
          {formattedLimit}
        </p>
      </div>

      {/* Brilho Sutil de Fundo (Simulando o reflexo da lâmina na cor da marca) */}
      <div className="absolute -right-6 -bottom-6 w-24 h-24 bg-brand/5 rounded-full blur-xl group-hover:bg-brand/10 transition-colors duration-300 pointer-events-none" />
    </div>
  );
}
