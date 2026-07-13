import { useState, useEffect } from "react";
import { useForm, useWatch } from "react-hook-form"; // ✨ AQUI: Importamos o useWatch
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAccounts } from "../../hooks/useAccounts";
import { useQueryClient } from "@tanstack/react-query";

import { Modal, ModalHeader, ModalBody, ModalFooter } from "../ui/Modal";
import { PickerModal } from "../ui/PickerModal";

interface Account {
  id: string;
  name: string;
  type: "checking" | "wallet" | "savings";
}

const accountSchema = z.object({
  name: z.string().min(2, "O nome da conta é obrigatório"),
  type: z.enum(["checking", "wallet", "savings"]),
});

type AccountForm = z.infer<typeof accountSchema>;

interface AccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  account?: Account | null;
}

export function AccountModal({ isOpen, onClose, account }: AccountModalProps) {
  const { createAccount, updateAccount, isCreating, isUpdating } =
    useAccounts();
  const queryClient = useQueryClient();

  const [isTypePickerOpen, setIsTypePickerOpen] = useState(false);
  const isEditMode = !!account;

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    control, // ✨ AQUI: Pegamos o control em vez do watch
    formState: { errors },
  } = useForm<AccountForm>({
    resolver: zodResolver(accountSchema),
    defaultValues: {
      name: "",
      type: "checking",
    },
  });

  useEffect(() => {
    if (account && isOpen) {
      reset({
        name: account.name,
        type: account.type,
      });
    } else if (!isOpen) {
      reset({ name: "", type: "checking" });
    }
  }, [account, isOpen, reset]);

  // ✨ AQUI: A forma segura (Compiler-Safe) de observar alterações
  const currentType = useWatch({
    control,
    name: "type",
  });

  const typeOptions = [
    { label: "🏛️ Conta Corrente (Bancos)", value: "checking" },
    { label: "💵 Carteira (Dinheiro Físico)", value: "wallet" },
    { label: "📈 Poupança / Investimentos", value: "savings" },
  ];

  const selectedTypeLabel =
    typeOptions.find((o) => o.value === currentType)?.label ||
    "Selecione o tipo...";

  if (!isOpen) return null;

  async function onSubmit(data: AccountForm): Promise<void> {
    try {
      if (isEditMode && updateAccount) {
        await updateAccount({ id: account.id, ...data });
      } else {
        await createAccount(data);
      }

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["accounts"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);

      onClose();
    } catch (error) {
      console.error("Erro ao salvar conta:", error);
      alert("Não foi possível guardar a conta. Tente novamente.");
    }
  }

  const isSubmitting = isCreating || isUpdating;

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} size="sm">
        <ModalHeader
          title={isEditMode ? "Editar Conta" : "Nova Conta ou Carteira"}
          onClose={onClose}
        />

        <form id="account-form" onSubmit={handleSubmit(onSubmit)}>
          <ModalBody className="p-6 space-y-5">
            <p className="text-xs font-medium text-muted -mt-2">
              {isEditMode
                ? "Altere as configurações desta fonte de liquidez"
                : "Adicione uma fonte de liquidez ao seu ecossistema"}
            </p>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Nome da Conta *
              </label>
              <input
                type="text"
                placeholder="Ex: Nubank, Itaú, Carteira..."
                {...register("name")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 hover:bg-surface focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
              />
              {errors.name && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.name.message}
                </span>
              )}
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                Tipo de Conta *
              </label>
              <button
                type="button"
                onClick={() => setIsTypePickerOpen(true)}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer ${
                  currentType ? "text-primary" : "text-muted"
                }`}
              >
                <span className="truncate">{selectedTypeLabel}</span>
              </button>
              {errors.type && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.type.message}
                </span>
              )}
            </div>
          </ModalBody>

          <ModalFooter>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 px-4 py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-sm font-bold transition-all shadow-2xs cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                form="account-form"
                className="flex-1 px-4 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer flex items-center justify-center"
              >
                {isSubmitting ? (
                  <span className="animate-pulse">A Guardar...</span>
                ) : (
                  "Guardar Conta"
                )}
              </button>
            </div>
          </ModalFooter>
        </form>
      </Modal>

      <PickerModal
        isOpen={isTypePickerOpen}
        onClose={() => setIsTypePickerOpen(false)}
        title="Tipo de Conta"
        options={typeOptions}
        selectedValue={currentType || ""}
        onSelect={(val) => {
          setValue("type", val as "checking" | "wallet" | "savings", {
            shouldValidate: true,
          });
          setIsTypePickerOpen(false);
        }}
      />
    </>
  );
}
