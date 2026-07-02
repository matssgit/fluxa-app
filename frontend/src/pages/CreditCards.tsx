import { useState } from "react";
import { Plus, ShoppingBag, CreditCard as CardIcon } from "lucide-react";
import { useCards, useDeleteCard, type Card } from "../hooks/useCredit";
import { CreateCardModal } from "../components/CreateCardModal";
import { CreatePurchaseModal } from "../components/CreatePurchaseModal";
import { EditCardModal } from "../components/EditCardModal";
import { CardItem } from "../components/CardItem";
import { CardDetailsModal } from "../components/CardDetailsModal";
import { DeleteActionModal } from "../components/DeleteActionModal";
import { Skeleton } from "../components/ui/Skeleton"; // Adicionamos a importação aqui!

export function CreditCards() {
   // Para testar o visual do esqueleto, mude isLoadingCards para `true` temporariamente
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

   // Ações de Navegação
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
      <div className="min-h-screen bg-slate-50 pb-12">
         <main className="max-w-6xl mx-auto px-6 py-15 space-y-8">
            {/* Cabeçalho */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
               <div>
                  <h2 className="text-2xl font-bold text-white sm:text-slate-800 sm:-mt-16">
                     Meus Cartões
                  </h2>
                  <p className="text-slate-400 sm:text-slate-500 text-sm mt-1">
                     Gerencie seus limites e faturas
                  </p>
               </div>

               <div className="flex gap-3 w-full sm:w-auto">
                  <button
                     onClick={() => setIsNewCardModalOpen(true)}
                     className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-slate-200 hover:bg-slate-300 text-slate-700 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm"
                  >
                     <Plus size={18} /> Novo Cartão
                  </button>
                  <button
                     onClick={() => setIsPurchaseModalOpen(true)}
                     className="flex-1 sm:flex-none flex items-center justify-center gap-2 bg-purple-600 hover:bg-purple-700 text-white px-4 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm"
                  >
                     <ShoppingBag size={18} /> Lançar Compra
                  </button>
               </div>
            </div>

            {/* Listagem de Cartões Componentizada */}
            {isLoadingCards ? (
               // 1. ESTADO DE LOADING (Skeletons substituindo o texto simples)
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {[1, 2, 3].map((i) => (
                     <div
                        key={i}
                        className="p-6 rounded-2xl border border-slate-100 bg-white h-48 flex flex-col justify-between shadow-sm"
                     >
                        <div className="flex justify-between items-start">
                           <Skeleton className="h-6 w-24 rounded-md" />
                           <Skeleton className="h-6 w-16 rounded-md" />
                        </div>
                        <div>
                           <Skeleton className="h-4 w-24 mb-2" />
                           <Skeleton className="h-8 w-32" />
                        </div>
                     </div>
                  ))}
               </div>
            ) : cards.length === 0 ? (
               // 2. EMPTY STATE (Mantido o seu, que já estava ótimo, só adicionei um subtítulo)
               <div className="bg-white rounded-2xl border border-slate-100 p-12 text-center shadow-sm">
                  <div className="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mx-auto mb-4">
                     <CardIcon size={32} />
                  </div>
                  <p className="text-slate-700 font-medium mb-1">
                     Você ainda não possui cartões cadastrados.
                  </p>
                  <p className="text-slate-400 text-sm">
                     Clique em "Novo Cartão" para começar a organizar seu
                     crédito.
                  </p>
               </div>
            ) : (
               // 3. ESTADO DE SUCESSO (Lista Real)
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {cards.map((card) => (
                     <CardItem
                        key={card.id}
                        card={card}
                        onClick={() => handleOpenDetails(card)}
                     />
                  ))}
               </div>
            )}

            {/* Modais Globais */}
            <CreateCardModal
               isOpen={isNewCardModalOpen}
               onClose={() => setIsNewCardModalOpen(false)}
            />
            <CreatePurchaseModal
               isOpen={isPurchaseModalOpen}
               onClose={() => setIsPurchaseModalOpen(false)}
            />

            {/* Modais Vinculados ao Cartão Selecionado */}
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
               warningText="O cartão só poderá ser excluído se não houver faturas abertas."
               onConfirm={async () => {
                  if (selectedCard) {
                     await deleteCard(selectedCard.id);
                     setIsDeleteModalOpen(false);
                     setSelectedCard(null);
                  }
               }}
            />
         </main>
      </div>
   );
}
