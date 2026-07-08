import { useState } from "react";
import { Plus, ShoppingBag } from "lucide-react";
import { useCards, useDeleteCard, type Card } from "../hooks/useCredit";
import { CreateCardModal } from "../components/cards/CreateCardModal";
import { CreatePurchaseModal } from "../components/CreatePurchaseModal";
import { EditCardModal } from "../components/cards/EditCardModal";
import { CardDetailsModal } from "../components/cards/CardDetailsModal";
import { DeleteActionModal } from "../components/transactions/DeleteActionModal";
import { CardsList } from "../components/cards/CardsList";

export function CreditCards() {
  const { data: cards = [], isLoading: isLoadingCards } = useCards();
  const { mutateAsync: deleteCard } = useDeleteCard();

  // Estados dos Modais Globais
  const [isNewCardModalOpen, setIsNewCardModalOpen] = useState(false);
  const [isPurchaseModalOpen, setIsPurchaseModalOpen] = useState(false);

  // Estados de Foco do Cartão
  const [selectedCard, setSelectedCard] = useState<Card | null>(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  // Ações de Navegação de Modais
  function handleOpenDetails(card: Card) {
    setSelectedCard(card);
    setIsDetailsModalOpen(true);
  }

  function handleOpenEdit() {
    setIsDetailsModalOpen(false);
    setIsEditModalOpen(true);
  }

  function handleOpenDelete() {
    setIsDetailsModalOpen(false);
    setIsDeleteModalOpen(true);
  }

  return (
    <div className="space-y-8 animate-fade-in">
      {/* 1. CABEÇALHO DE AÇÕES */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-xl lg:text-2xl font-bold text-primary tracking-tight">
            Meus Cartões
          </h2>
          <p className="text-muted text-xs sm:text-sm font-medium mt-0.5">
            Gerencie seus limites, datas de fechamento e faturas ativas
          </p>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          <button
            onClick={() => setIsNewCardModalOpen(true)}
            className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-elevated hover:bg-subtle/40 text-primary px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-2xs border border-subtle/30 cursor-pointer"
          >
            <Plus size={16} className="text-brand" />
            <span>Novo Cartão</span>
          </button>

          <button
            onClick={() => setIsPurchaseModalOpen(true)}
            className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all duration-200 shadow-sm cursor-pointer"
          >
            <ShoppingBag size={16} />
            <span>Lançar Compra</span>
          </button>
        </div>
      </div>

      {/* 2. LISTAGEM DE CARTÕES METALIZADOS */}
      <CardsList
        cards={cards}
        isLoading={isLoadingCards}
        onSelectCard={handleOpenDetails}
        onNewCardClick={() => setIsNewCardModalOpen(true)}
      />

      {/* 3. MODAIS GLOBAIS */}
      <CreateCardModal
        isOpen={isNewCardModalOpen}
        onClose={() => setIsNewCardModalOpen(false)}
      />
      <CreatePurchaseModal
        isOpen={isPurchaseModalOpen}
        onClose={() => setIsPurchaseModalOpen(false)}
      />

      {/* 4. MODAIS DE CONTEXTO DO CARTÃO SELECIONADO */}
      <CardDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => setIsDetailsModalOpen(false)}
        card={selectedCard}
        onEditClick={handleOpenEdit}
        onDeleteClick={handleOpenDelete}
      />

      <EditCardModal
        key={`edit-${selectedCard?.id}`}
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setIsDetailsModalOpen(true);
        }}
        card={selectedCard}
      />

      <DeleteActionModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setIsDetailsModalOpen(true);
        }}
        title="Excluir Cartão"
        description={`Você tem certeza que deseja excluir o cartão ${selectedCard?.name}?`}
        warningText="O cartão só poderá ser excluído se não houver faturas ou parcelas abertas."
        onConfirm={async () => {
          if (selectedCard) {
            await deleteCard(selectedCard.id);
            setIsDeleteModalOpen(false);
            setSelectedCard(null);
          }
        }}
      />
    </div>
  );
}
