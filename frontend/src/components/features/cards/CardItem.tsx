import { WalletCards } from "lucide-react";
import { PrivacyMask } from "../../ui/PrivacyMask";
import { type Card } from "../../../hooks/useCredit";

interface CardItemProps {
  card: Card;
  onClick: () => void;
}

export function CardItem({ card, onClick }: CardItemProps) {
  const cardColor = card.color || "#10B981";
  const totalLimit = Number(card.total_limit || 0);
  const availableLimit = Number(card.available_limit || 0);
  const usedPercentage =
    totalLimit > 0
      ? Math.min(100, Math.max(0, ((totalLimit - availableLimit) / totalLimit) * 100))
      : 0;

  return (
    <div
      onClick={onClick}
      className="group flex flex-col justify-between aspect-[1.58/1] min-h-56 max-h-72 relative overflow-hidden p-6 sm:p-7 rounded-tl-[2.5rem] rounded-br-[2.5rem] rounded-tr-xl rounded-bl-xl bg-[#173f36] text-[#fffdf7] transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-md cursor-pointer shadow-sm border border-white/5"
    >
      <div
        className="absolute -right-16 -top-20 w-56 h-56 rounded-full border-[34px] border-white/5 transition-opacity duration-300 opacity-70 pointer-events-none"
        style={{ backgroundColor: cardColor }}
      />

      <div className="flex justify-between items-start mb-6 z-10">
        <div
          className="w-11 h-11 rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform duration-200 shadow-xs"
          style={{
            backgroundColor: `${cardColor}35`,
            color: "#fffdf7",
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

      <div className="space-y-1.5 z-10">
        <h3 className="font-bold text-[#dfe8de] text-base tracking-tight truncate">
          {card.name}
        </h3>
        <p className="text-[10px] font-extrabold text-[#dfe8de]/55 uppercase tracking-[0.15em]">
          Limite Disponível
        </p>
        <p className="text-2xl sm:text-3xl font-extrabold tracking-[-0.05em] tabular-nums">
          <PrivacyMask amount={availableLimit} />
        </p>
        <div className="mt-4 h-1.5 bg-white/10 rounded-full overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-500"
            style={{ width: `${usedPercentage}%`, backgroundColor: cardColor }}
          />
        </div>
        <div className="flex justify-between mt-2 text-[9px] font-bold uppercase tracking-wider text-[#dfe8de]/55">
          <span>{usedPercentage.toFixed(0)}% utilizado</span>
          <span>Vence dia {card.due_day}</span>
        </div>
      </div>

      <div
        className="absolute -left-12 -bottom-16 w-40 h-40 rounded-[65%_35%_55%_45%] blur-2xl transition-opacity duration-300 opacity-15 group-hover:opacity-25 pointer-events-none"
        style={{ backgroundColor: cardColor }}
      />
    </div>
  );
}
