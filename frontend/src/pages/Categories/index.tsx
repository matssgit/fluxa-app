import { useState } from "react";
import { useCategories } from "../../hooks/useCategories";
import { CategoryModal } from "../../components/transactions/CategoryModal";
import { Tag, TrendingUp, TrendingDown, Edit2, Trash2 } from "lucide-react";
import { DeleteActionModal } from "../../components/transactions/DeleteActionModal";
import { FeatureIntroduction } from "../../components/ui/EmptyState/FeatureIntroduction";

interface CategoryData {
  id: string;
  name: string;
  type: string;
}

export function Categories() {
  const { categories = [], isLoading, deleteCategory } = useCategories();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [categoryToEdit, setCategoryToEdit] = useState<CategoryData | null>(
    null,
  );
  const [categoryToDelete, setCategoryToDelete] = useState<CategoryData | null>(
    null,
  );

  const incomeCategories = categories.filter(
    (c: CategoryData) => c.type === "income" || c.type === "entrada",
  );
  const expenseCategories = categories.filter(
    (c: CategoryData) => c.type === "expense" || c.type === "saida",
  );

  const handleEdit = (cat: CategoryData) => {
    setCategoryToEdit(cat);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setCategoryToEdit(null);
  };

  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return;
    try {
      await deleteCategory(categoryToDelete.id);
      setCategoryToDelete(null);
    } catch (error) {
      console.error("Erro ao deletar categoria:", error);
      alert(
        "Não é possível deletar esta categoria. Ela pode estar vinculada a lançamentos existentes.",
      );
    }
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in pb-20 sm:pb-8">
      {categories.length > 0 && !isLoading && (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-primary tracking-tight">
              Categorias
            </h1>
            <p className="text-xs sm:text-sm font-medium text-muted mt-1">
              Organize e classifique as suas transações.
            </p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold transition-all shadow-sm cursor-pointer active:scale-95"
          >
            <Tag size={18} />
            <span>Nova Categoria</span>
          </button>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand"></div>
        </div>
      ) : categories.length === 0 ? (
        <FeatureIntroduction
          icon={Tag}
          title="Categorias"
          subtitle="Para onde vai o seu dinheiro?"
          whatIs="Categorias servem para identificar de onde veio ou para onde foi o seu dinheiro. Elas ajudam o Fluxa a organizar as suas movimentações automaticamente e gerar relatórios precisos."
          examples={[
            { label: "Salário / Freelance", category: "Receita" },
            { label: "Supermercado", category: "Despesa" },
            { label: "Transporte / Combustível", category: "Despesa" },
            { label: "Netflix / Spotify", category: "Despesa" },
          ]}
          tip="Você precisa de pelo menos uma categoria antes de começar a lançar os seus gastos ou assinaturas."
          actionLabel="Criar a minha primeira Categoria"
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-surface border border-subtle/30 rounded-3xl p-5 sm:p-6 shadow-sm flex flex-col h-full">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-2xl bg-red-500/10 text-red-500 flex items-center justify-center shrink-0">
                <TrendingDown size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-primary tracking-tight">
                  Saídas
                </h2>
                <p className="text-[11px] font-medium text-muted">
                  Despesas e gastos
                </p>
              </div>
            </div>

            <div className="flex-1 space-y-2">
              {expenseCategories.length === 0 ? (
                <p className="text-sm text-muted text-center py-4">
                  Nenhuma categoria de saída.
                </p>
              ) : (
                expenseCategories.map((cat: CategoryData) => (
                  <div
                    key={cat.id}
                    className="group flex items-center justify-between p-3.5 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-elevated transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold text-secondary group-hover:text-primary transition-colors">
                        {cat.name}
                      </span>
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-red-500 bg-red-500/10 px-2 py-1 rounded-lg">
                        Despesa
                      </span>
                    </div>

                    <div className="flex items-center gap-1 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleEdit(cat)}
                        className="p-2 text-muted hover:text-brand hover:bg-brand/10 rounded-xl transition-colors cursor-pointer"
                        title="Editar Categoria"
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        onClick={() => setCategoryToDelete(cat)}
                        className="p-2 text-muted hover:text-red-500 hover:bg-red-500/10 rounded-xl transition-colors cursor-pointer"
                        title="Excluir Categoria"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="bg-surface border border-subtle/30 rounded-3xl p-5 sm:p-6 shadow-sm flex flex-col h-full">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-2xl bg-emerald-500/10 text-emerald-500 flex items-center justify-center shrink-0">
                <TrendingUp size={20} />
              </div>
              <div>
                <h2 className="text-base font-bold text-primary tracking-tight">
                  Entradas
                </h2>
                <p className="text-[11px] font-medium text-muted">
                  Receitas e ganhos
                </p>
              </div>
            </div>

            <div className="flex-1 space-y-2">
              {incomeCategories.length === 0 ? (
                <p className="text-sm text-muted text-center py-4">
                  Nenhuma categoria de entrada.
                </p>
              ) : (
                incomeCategories.map((cat: CategoryData) => (
                  <div
                    key={cat.id}
                    className="group flex items-center justify-between p-3.5 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-elevated transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold text-secondary group-hover:text-primary transition-colors">
                        {cat.name}
                      </span>
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-emerald-500 bg-emerald-500/10 px-2 py-1 rounded-lg">
                        Receita
                      </span>
                    </div>

                    <div className="flex items-center gap-1 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleEdit(cat)}
                        className="p-2 text-muted hover:text-brand hover:bg-brand/10 rounded-xl transition-colors cursor-pointer"
                        title="Editar Categoria"
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        onClick={() => setCategoryToDelete(cat)}
                        className="p-2 text-muted hover:text-red-500 hover:bg-red-500/10 rounded-xl transition-colors cursor-pointer"
                        title="Excluir Categoria"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      <CategoryModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        initialData={categoryToEdit}
      />

      <DeleteActionModal
        isOpen={!!categoryToDelete}
        onClose={() => setCategoryToDelete(null)}
        onConfirm={handleConfirmDelete}
        title="Excluir Categoria"
        description={
          categoryToDelete
            ? `Tem certeza que deseja excluir a categoria "${categoryToDelete.name}"? Esta ação não pode ser desfeita.`
            : ""
        }
      />
    </div>
  );
}
