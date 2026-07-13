import { useState } from "react";
import { Plus, ShoppingBag } from "lucide-react";
import { useCards, useDeleteCard } from "../../hooks/useCredit";
import { CreateCardModal } from "../../components/cards/CreateCardModal";
import { CreatePurchaseModal } from "../../components/cards/CreatePurchaseModal";
import { EditCardModal } from "../../components/cards/EditCardModal";
import { CardDetailsModal } from "../../components/cards/CardDetailsModal";
import { DeleteActionModal } from "../../components/transactions/DeleteActionModal";
import { CardsList } from "../../components/cards/CardsList";

export function CreditCards() {
  const { data: cards = [], isLoading: isLoadingCards } = useCards();
  const { mutateAsync: deleteCard } = useDeleteCard();

  const [isNewCardModalOpen, setIsNewCardModalOpen] = useState(false);
  const [isPurchaseModalOpen, setIsPurchaseModalOpen] = useState(false);
  const [selectedCardId, setSelectedCardId] = useState<string | null>(null);

  const activeCard = cards.find((c) => c.id === selectedCardId) || null;

  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  function handleOpenDetails(cardId: string) {
    setSelectedCardId(cardId);
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
    <div className="w-full space-y-6 sm:space-y-8">
      {/* 🚀 REGRA 16: Título removido. Começamos direto com as Ações! */}
      <div className="flex flex-col sm:flex-row justify-start items-center gap-3 w-full">
        <button
          onClick={() => setIsNewCardModalOpen(true)}
          className="w-full sm:w-auto flex items-center justify-center gap-2 bg-elevated hover:bg-subtle/40 text-primary px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-2xs border border-subtle/30 cursor-pointer"
        >
          <Plus size={16} className="text-brand" />
          <span>Novo Cartão</span>
        </button>

        <button
          onClick={() => setIsPurchaseModalOpen(true)}
          className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-sm cursor-pointer"
        >
          <ShoppingBag size={16} />
          <span>Lançar Compra</span>
        </button>
      </div>

      <CardsList
        cards={cards}
        isLoading={isLoadingCards}
        onSelectCard={(card) => handleOpenDetails(card.id)}
        onNewCardClick={() => setIsNewCardModalOpen(true)}
      />

      {/* MODAIS DO ECOSSISTEMA DE CRÉDITO */}
      <CreateCardModal
        isOpen={isNewCardModalOpen}
        onClose={() => setIsNewCardModalOpen(false)}
      />

      <CreatePurchaseModal
        isOpen={isPurchaseModalOpen}
        onClose={() => setIsPurchaseModalOpen(false)}
      />

      <CardDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => {
          setIsDetailsModalOpen(false);
          setSelectedCardId(null);
        }}
        card={activeCard}
        onEditClick={handleOpenEdit}
        onDeleteClick={handleOpenDelete}
      />

      <EditCardModal
        key={`edit-${activeCard?.id}`}
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          if (activeCard) setIsDetailsModalOpen(true);
        }}
        card={activeCard}
      />

      <DeleteActionModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          if (activeCard) setIsDetailsModalOpen(true);
        }}
        title="Excluir Cartão"
        description={`Você tem certeza que deseja excluir o cartão ${activeCard?.name}?`}
        warningText="O cartão só poderá ser excluído se não houver faturas ou parcelas abertas."
        onConfirm={async () => {
          if (!activeCard) return;
          try {
            await deleteCard(activeCard.id);
            setIsDeleteModalOpen(false);
            setIsDetailsModalOpen(false);
            setSelectedCardId(null);
          } catch (error: unknown) {
            console.error("Erro na exclusão do cartão:", error);
            alert(
              "⚠️ Exclusão Bloqueada por Segurança!\n\nEste cartão possui compras parceladas ou movimentações registradas no seu histórico. Para proteger a integridade dos seus relatórios, cancele as compras vinculadas a ele antes de excluí-lo.",
            );
          }
        }}
      />
    </div>
  );
}
