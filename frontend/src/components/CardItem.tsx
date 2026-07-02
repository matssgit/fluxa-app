import { type Card } from "../hooks/useCredit";
import { CreditCard } from "lucide-react";

interface CardItemProps {
   card: Card;
   onClick: () => void; // A prop que estava faltando
}

export function CardItem({ card, onClick }: CardItemProps) {
   return (
      <div
         onClick={onClick}
         className="bg-premium-card p-6 rounded-2xl cursor-pointer hover:translate-y-[-2px] transition-all duration-300 group border border-finance-primary/5 shadow-premium"
      >
         <div className="flex justify-between items-start mb-6">
            <div className="bg-finance-secondary/5 p-2 rounded-lg border border-finance-secondary/10">
               <CreditCard size={20} className="text-finance-secondary" />
            </div>
            <span className="text-[10px] font-bold uppercase tracking-widest text-finance-primary/40 border border-finance-primary/10 px-2 py-1 rounded-md">
               {card.brand}
            </span>
         </div>

         <div className="space-y-1">
            <h3 className="font-bold text-finance-primary text-base">
               {card.name}
            </h3>
            <p className="text-[11px] font-bold text-finance-primary/40 uppercase tracking-widest">
               Limite Disponível
            </p>
            <p className="text-xl font-bold text-finance-primary tracking-tight">
               {new Intl.NumberFormat("pt-BR", {
                  style: "currency",
                  currency: "BRL",
               }).format(Number(card.available_limit || 0))}
            </p>
         </div>
      </div>
   );
}
