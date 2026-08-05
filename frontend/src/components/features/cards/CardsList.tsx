import { CardItem } from "./CardItem";
import { Skeleton } from "../../ui/Skeleton";
import { CreditCard } from "lucide-react";
import { EmptyState } from "../../ui/EmptyState";
import { type Card } from "../../../hooks/useCredit";

interface CardsListProps {
  cards: Card[];
  isLoading: boolean;
  onSelectCard: (card: Card) => void;
  onNewCardClick: () => void;
}

export function CardsList({
  cards,
  isLoading,
  onSelectCard,
  onNewCardClick,
}: CardsListProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="card-default min-h-40 flex flex-col justify-between"
          >
            <div className="flex justify-between items-start">
              <Skeleton className="h-10 w-10 rounded-xl" />
              <Skeleton className="h-6 w-16 rounded-md" />
            </div>
            <div className="space-y-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-7 w-32" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (cards.length === 0) {
    return (
      <div className="card-default py-12 flex flex-col items-center justify-center text-center">
        <EmptyState
          icon={CreditCard}
          title="Nenhum cartão cadastrado"
          description="Adicione seu primeiro cartão de crédito para começar a organizar seus limites, compras parceladas e faturas."
        />

        {/* Botão de ação posicionado abaixo do estado vazio */}
        <button
          onClick={onNewCardClick}
          className="mt-6 px-5 py-2.5 bg-brand hover:bg-brand-light text-white text-xs sm:text-sm font-bold rounded-xl transition-all duration-200 shadow-sm cursor-pointer"
        >
          Adicionar Cartão
        </button>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {cards.map((card) => (
        <CardItem
          key={card.id}
          card={card}
          onClick={() => onSelectCard(card)}
        />
      ))}
    </div>
  );
}
