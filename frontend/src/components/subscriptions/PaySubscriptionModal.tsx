import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAccounts } from "../../hooks/useAccounts";
import { usePaySubscription } from "../../hooks/useSubscriptions";

// Importação da nossa nova Biblioteca de Componentes!
import { Modal, ModalHeader, ModalBody, ModalFooter } from "../ui/Modal";

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

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PayForm>({
    resolver: zodResolver(paySchema),
  });

  async function onSubmit(data: PayForm) {
    if (!subscriptionId) return;
    try {
      await paySub({ id: subscriptionId, account_id: data.account_id });
      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao registrar pagamento da assinatura", error);
    }
  }

  return (
    <Modal isOpen={isOpen && !!subscriptionId} onClose={onClose} size="md">
      <ModalHeader
        title="Baixar Assinatura"
        description="Confirme a conta utilizada para este pagamento recorrente."
        onClose={onClose}
      />

      <form onSubmit={handleSubmit(onSubmit)}>
        <ModalBody>
          <div>
            <label className="block text-sm font-semibold text-primary mb-2">
              De qual conta o dinheiro saiu?
            </label>
            <select
              {...register("account_id")}
              className={`input cursor-pointer ${errors.account_id ? "input-error" : ""}`}
            >
              <option value="">Selecione a conta...</option>
              {accounts?.map((acc: { id: string; name: string }) => (
                <option key={acc.id} value={acc.id}>
                  {acc.name}
                </option>
              ))}
            </select>
            {errors.account_id && (
              <span className="text-danger text-xs font-medium mt-1.5 block">
                {errors.account_id.message}
              </span>
            )}
          </div>
        </ModalBody>

        <ModalFooter>
          <button type="button" onClick={onClose} className="btn-secondary">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isPending}
            className="btn-primary w-full sm:w-auto"
          >
            {isPending ? "Processando..." : "Confirmar Pagamento"}
          </button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
