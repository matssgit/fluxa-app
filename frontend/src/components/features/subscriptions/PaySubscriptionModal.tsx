import { z } from "zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Wallet, CheckCircle2 } from "lucide-react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAccounts } from "../../../hooks/useAccounts";
import { usePaySubscription } from "../../../hooks/useSubscriptions";
import { Modal, ModalHeader, ModalBody, ModalFooter } from "../../ui/Modal";

const paySchema = z.object({
  account_id: z.string().uuid("Selecione uma conta para continuar"),
});

type PayForm = z.infer<typeof paySchema>;

interface Props {
  isOpen: boolean;
  onClose: () => void;
  subscriptionId: string | null;
}

export function PaySubscriptionModal({
  isOpen,
  onClose,
  subscriptionId,
}: Props) {
  const { accounts } = useAccounts();
  const { mutateAsync: paySub, isPending } = usePaySubscription();

  const [selectedAccountId, setSelectedAccountId] = useState<string>("");

  const {
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<PayForm>({
    resolver: zodResolver(paySchema),
  });

  const handleClose = () => {
    reset();
    setSelectedAccountId("");
    onClose();
  };

  async function onSubmit(data: PayForm) {
    if (!subscriptionId) return;
    try {
      await paySub({ id: subscriptionId, account_id: data.account_id });
      handleClose();
    } catch (error) {
      console.error("Erro ao registrar pagamento da assinatura", error);
    }
  }

  return (
    <Modal isOpen={isOpen && !!subscriptionId} onClose={handleClose} size="md">
      <ModalHeader
        title="Baixar Assinatura"
        description="Confirme a conta utilizada para este pagamento recorrente."
        onClose={handleClose}
      />

      <form onSubmit={handleSubmit(onSubmit)}>
        <ModalBody className="max-h-[65dvh] overflow-y-auto">
          <div className="space-y-4">
            <label className="block text-sm font-bold text-primary">
              De qual conta o dinheiro saiu?
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {accounts?.map((acc: { id: string; name: string }) => {
                const isSelected = selectedAccountId === acc.id;

                return (
                  <div
                    key={acc.id}
                    onClick={() => {
                      setValue("account_id", acc.id, { shouldValidate: true });
                      setSelectedAccountId(acc.id); // Sincroniza o visual com o formulário
                    }}
                    className={`p-4 rounded-2xl border cursor-pointer transition-all flex items-center justify-between group ${
                      isSelected
                        ? "border-brand bg-brand/5 ring-1 ring-brand"
                        : "border-subtle/20 bg-surface hover:border-subtle/40 hover:bg-elevated/50"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-10 h-10 rounded-xl flex items-center justify-center transition-colors ${
                          isSelected
                            ? "bg-brand/10 text-brand"
                            : "bg-elevated text-muted group-hover:text-primary"
                        }`}
                      >
                        <Wallet size={18} />
                      </div>
                      <span
                        className={`text-sm font-bold ${
                          isSelected
                            ? "text-primary"
                            : "text-secondary group-hover:text-primary"
                        }`}
                      >
                        {acc.name}
                      </span>
                    </div>

                    <div className="shrink-0 flex items-center justify-center">
                      {isSelected ? (
                        <CheckCircle2
                          size={20}
                          className="text-brand animate-scale-in"
                        />
                      ) : (
                        <div className="w-5 h-5 rounded-full border-2 border-subtle/30 group-hover:border-subtle/50 transition-colors" />
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {errors.account_id && (
              <span className="text-red-500 text-xs font-bold mt-2  animate-fade-in flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 shrink-0" />
                {errors.account_id.message}
              </span>
            )}
          </div>
        </ModalBody>

        <ModalFooter>
          <button
            type="button"
            onClick={handleClose}
            className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-sm bg-surface border border-subtle/30 text-secondary hover:text-primary hover:bg-elevated transition-colors cursor-pointer"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-sm bg-brand text-white shadow-sm hover:bg-brand-light transition-colors active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 cursor-pointer"
          >
            {isPending ? (
              <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Processando...
              </>
            ) : (
              "Confirmar Pagamento"
            )}
          </button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
