import { CreditCard } from "lucide-react";
import { useCards } from "../hooks/useCredit";
import { Skeleton } from "./ui/Skeleton"; // Importação arrumada apontando para a pasta UI

export function CardsSection() {
  const { data: cards = [], isLoading } = useCards();

  return (
    <div className="mt-8">
      <h2 className="text-lg font-semibold text-primary mb-4 tracking-tight">
        Meus Cartões
      </h2>

      {/* ESTADO 1: LOADING (Skeletons) */}
      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="card-default h-48 flex flex-col justify-between"
            >
              <div className="flex justify-between items-start">
                <Skeleton className="h-10 w-10 rounded-full" />
                <Skeleton className="h-6 w-16 rounded-full" />
              </div>
              <div>
                <Skeleton className="h-4 w-24 mb-2" />
                <Skeleton className="h-8 w-32" />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ESTADO 2: EMPTY STATE (Sem cartões) */}
      {!isLoading && cards.length === 0 && (
        <div className="flex flex-col items-center justify-center p-10 bg-elevated border border-subtle rounded-2xl text-center">
          <div className="w-16 h-16 bg-surface shadow-sm rounded-full flex items-center justify-center text-muted mb-4 border border-subtle">
            <CreditCard size={32} />
          </div>
          <h3 className="text-primary font-semibold mb-1 tracking-tight">
            Nenhum cartão cadastrado
          </h3>
          <p className="text-secondary text-sm max-w-xs">
            Adicione seu primeiro cartão de crédito para começar a controlar
            seus limites e parcelamentos.
          </p>
        </div>
      )}

      {/* ESTADO 3: SUCESSO (Lista os cartões reais) */}
      {!isLoading && cards.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {cards.map((card) => (
            <div key={card.id} className="card-interactive">
              <span className="font-semibold text-primary">{card.name}</span>
              <span className="text-muted ml-2 text-sm">{card.brand}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
