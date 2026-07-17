import { useState } from "react";
import { Plus, ShoppingBag, CreditCard } from "lucide-react";
import { useCards, useDeleteCard } from "../../hooks/useCredit";
import { CreateCardModal } from "../../components/cards/CreateCardModal";
import { CreatePurchaseModal } from "../../components/cards/CreatePurchaseModal";
import { EditCardModal } from "../../components/cards/EditCardModal";
import { CardDetailsModal } from "../../components/cards/CardDetailsModal";
import { DeleteActionModal } from "../../components/transactions/DeleteActionModal";
import { CardsList } from "../../components/cards/CardsList";
import { FeatureIntroduction } from "../../components/ui/EmptyState/FeatureIntroduction";

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
    <div className="w-full space-y-6 sm:space-y-8 animate-fade-in pb-20 sm:pb-8">
      {/* 🚀 REGRA DE UX #04: O Respiro e o Contexto (Exibido se já existirem cartões) */}
      {!isLoadingCards && cards.length > 0 && (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Cartões
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Controle limites, compras parceladas e faturas.
            </p>
          </div>

          {/* Botões de Ação integrados hierarquicamente */}
          <div className="flex flex-col sm:flex-row items-center gap-3 w-full sm:w-auto mt-2 sm:mt-0">
            <button
              onClick={() => setIsNewCardModalOpen(true)}
              className="w-full sm:w-auto flex items-center justify-center gap-2 bg-elevated hover:bg-subtle/40 text-primary px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-2xs border border-subtle/30 cursor-pointer"
            >
              <Plus size={16} className="text-brand" />
              <span>Novo Cartão</span>
            </button>

            <button
              onClick={() => setIsPurchaseModalOpen(true)}
              className="w-full sm:w-auto flex items-center justify-center gap-2 bg-brand hover:bg-brand-light text-white px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition-all shadow-sm cursor-pointer active:scale-95"
            >
              <ShoppingBag size={16} />
              <span>Lançar Compra</span>
            </button>
          </div>
        </div>
      )}

      {/* UX EDUCACIONAL SE ESTIVER VAZIO */}
      {!isLoadingCards && cards.length === 0 ? (
        <FeatureIntroduction
          icon={CreditCard}
          title="Cartões de Crédito"
          subtitle="O controlo total das suas faturas"
          whatIs="Cadastre aqui os seus cartões de crédito. Depois você poderá registar compras parceladas, acompanhar o limite disponível em tempo real e controlar o pagamento de cada fatura automaticamente."
          examples={[
            { label: "Nubank", category: "Roxinho" },
            { label: "Itaú", category: "Click / Uniclass" },
            { label: "C6 Bank", category: "Carbon" },
            { label: "Inter", category: "Mastercard" },
          ]}
          tip="O valor da fatura só é descontado do seu Caixa (Conta Corrente) no dia em que você regista o pagamento dela no sistema."
          actionLabel="Cadastrar o meu primeiro Cartão"
          onAction={() => setIsNewCardModalOpen(true)}
        />
      ) : (
        <CardsList
          cards={cards}
          isLoading={isLoadingCards}
          onSelectCard={(card) => handleOpenDetails(card.id)}
          onNewCardClick={() => setIsNewCardModalOpen(true)}
        />
      )}

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
