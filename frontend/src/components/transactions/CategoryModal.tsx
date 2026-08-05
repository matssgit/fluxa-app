import { z } from "zod";
import { X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useCategories } from "../../hooks/useCategories";
import { Modal, ModalBody, ModalFooter } from "../ui/Modal";

const categorySchema = z.object({
  name: z.string().min(2, "O nome da categoria é obrigatório"),
  type: z.enum(["income", "expense"]),
});

type CategoryForm = z.infer<typeof categorySchema>;

interface CategoryData {
  id: string;
  name: string;
  type: "income" | "expense" | string;
}

interface CategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialData?: CategoryData | null;
}

export function CategoryModal({
  isOpen,
  onClose,
  initialData,
}: CategoryModalProps) {
  const { createCategory, isCreating, updateCategory, isUpdating } =
    useCategories();
  const isEditMode = !!initialData;
  const isSaving = isCreating || isUpdating;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryForm>({
    resolver: zodResolver(categorySchema),
    defaultValues: { type: "expense" },
  });

  // Preenche o formulário se houver dados iniciais, ou limpa-o para criação
  useEffect(() => {
    if (isOpen && initialData) {
      reset({
        name: initialData.name,
        type: (initialData.type === "entrada"
          ? "income"
          : initialData.type === "saida"
            ? "expense"
            : initialData.type) as "income" | "expense",
      });
    } else if (isOpen && !initialData) {
      reset({ name: "", type: "expense" });
    }
  }, [isOpen, initialData, reset]);

  if (!isOpen) return null;

  async function onSubmit(data: CategoryForm) {
    try {
      if (isEditMode && initialData) {
        await updateCategory({ id: initialData.id, data });
      } else {
        await createCategory(data);
      }
      onClose();
    } catch (error) {
      console.error("Erro ao salvar categoria", error);
      alert("Erro ao salvar categoria.");
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="md">
      <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
        <h2 className="text-lg sm:text-xl font-bold text-primary tracking-tight">
          {isEditMode ? "Editar Categoria" : "Nova Categoria"}
        </h2>
        <button
          onClick={onClose}
          className="p-2 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer"
        >
          <X size={18} />
        </button>
      </div>

      <ModalBody>
        <form
          id="category-form"
          onSubmit={handleSubmit(onSubmit)}
          className="space-y-5"
        >
          <div className="flex gap-4 p-1 bg-elevated/50 rounded-xl border border-subtle/30">
            <label className="flex-1 cursor-pointer">
              <input
                type="radio"
                value="expense"
                {...register("type")}
                className="peer sr-only"
              />
              <div className="text-center py-2 rounded-lg text-sm font-bold text-secondary peer-checked:bg-surface peer-checked:text-red-500 peer-checked:shadow-sm transition-all">
                Saída (Despesa)
              </div>
            </label>
            <label className="flex-1 cursor-pointer">
              <input
                type="radio"
                value="income"
                {...register("type")}
                className="peer sr-only"
              />
              <div className="text-center py-2 rounded-lg text-sm font-bold text-secondary peer-checked:bg-surface peer-checked:text-emerald-500 peer-checked:shadow-sm transition-all">
                Entrada (Receita)
              </div>
            </label>
          </div>

          <div>
            <label className="block text-sm font-bold text-primary mb-1">
              Nome da Categoria
            </label>
            <input
              type="text"
              placeholder="Ex: Supermercado, Salário, Lazer..."
              {...register("name")}
              className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary outline-none transition-all"
            />
            {errors.name && (
              <span className="text-red-500 text-xs mt-1 block">
                {errors.name.message}
              </span>
            )}
          </div>
        </form>
      </ModalBody>

      <ModalFooter>
        <button
          type="button"
          onClick={onClose}
          className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl border border-subtle/30 text-secondary font-bold hover:bg-elevated transition-colors cursor-pointer"
        >
          Cancelar
        </button>
        <button
          type="submit"
          form="category-form"
          disabled={isSaving}
          className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl bg-brand text-white font-bold hover:bg-brand-light disabled:opacity-70 transition-colors cursor-pointer"
        >
          {isSaving ? "Salvando..." : "Salvar"}
        </button>
      </ModalFooter>
    </Modal>
  );
}
