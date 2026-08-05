import { z } from "zod";
import { useState } from "react";
import { useCards } from "../../../hooks/useCredit";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAccounts } from "../../../hooks/useAccounts";
import { useQueryClient } from "@tanstack/react-query";
import { useCategories } from "../../../hooks/useCategories";
import { useCreateSubscription } from "../../../hooks/useSubscriptions";
import {
  DollarSign,
  Tag,
  Building,
  CreditCard,
  CalendarDays,
  ChevronDown,
} from "lucide-react";

import {
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "../../../components/ui/Modal";
import { PickerModal } from "../../../components/ui/PickerModal";

interface Category {
  id: string;
  name: string;
  type: string;
}

interface Account {
  id: string;
  name: string;
}

interface Card {
  id: string;
  name: string;
}

const subscriptionSchema = z.object({
  title: z.string().min(1, "O nome do serviço é obrigatório"),
  amount: z.number().min(0.01, "O valor deve ser maior que zero"),
  due_day: z.number().min(1, "Dia inválido").max(31, "Dia inválido"),
  frequency: z.enum(["monthly", "yearly"]),
  category_id: z.string().min(1, "Selecione uma categoria"),
  payment_method: z.enum(["account", "card"]),
  account_id: z.string().optional(),
  card_id: z.string().optional(),
});

type SubscriptionForm = z.infer<typeof subscriptionSchema>;

interface CreateSubscriptionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateSubscriptionModal({
  isOpen,
  onClose,
}: CreateSubscriptionModalProps) {
  const { mutateAsync: createSub, isPending } = useCreateSubscription();
  const { accounts = [] } = useAccounts();
  const { categories = [] } = useCategories();
  const { data: cards = [] } = useCards();
  const queryClient = useQueryClient();

  const [isCategoryPickerOpen, setIsCategoryPickerOpen] = useState(false);
  const [isFrequencyPickerOpen, setIsFrequencyPickerOpen] = useState(false);
  const [isDueDayPickerOpen, setIsDueDayPickerOpen] = useState(false);
  const [isPaymentPickerOpen, setIsPaymentPickerOpen] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    control,
    reset,
    formState: { errors },
  } = useForm<SubscriptionForm>({
    resolver: zodResolver(subscriptionSchema),
    defaultValues: { frequency: "monthly", payment_method: "account" },
  });

  const paymentMethod = useWatch({ control, name: "payment_method" });
  const currentCategoryId = useWatch({ control, name: "category_id" });
  const currentFrequency = useWatch({ control, name: "frequency" });
  const currentAccountId = useWatch({ control, name: "account_id" });
  const currentCardId = useWatch({ control, name: "card_id" });
  const currentDueDay = useWatch({ control, name: "due_day" });

  const filteredCategories = (categories as Category[]).filter(
    (c) => c.type === "expense" || c.type === "saida",
  );

  const selectedCategoryName =
    filteredCategories.find((c) => c.id === currentCategoryId)?.name ||
    "Selecione...";

  const selectedPaymentName =
    paymentMethod === "account"
      ? (accounts as Account[]).find((a) => a.id === currentAccountId)?.name ||
        "Selecione a conta..."
      : (cards as Card[]).find((c) => c.id === currentCardId)?.name ||
        "Selecione o cartão...";

  if (!isOpen) return null;

  async function onSubmit(data: SubscriptionForm): Promise<void> {
    // RASTREADOR 2: Ver o que chegou no Submit
    console.log("FORM DATA", data);
    console.log("ACCOUNT", data.account_id);
    console.log("CARD", data.card_id);

    const formData = getValues();
    const finalAccountId =
      formData.payment_method === "account" ? formData.account_id : undefined;
    const finalCardId =
      formData.payment_method === "card" ? formData.card_id : undefined;

    if (!finalAccountId && !finalCardId) {
      alert(
        "Atenção: Selecione a Conta Bancária ou o Cartão antes de guardar!",
      );
      return;
    }

    try {
      await createSub({
        title: data.title,
        amount: data.amount,
        due_day: data.due_day,
        frequency: data.frequency,
        category_id: data.category_id,
        account_id: finalAccountId,
        card_id: finalCardId,
      });

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["subscriptions"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      ]);

      reset();
      onClose();
    } catch (error) {
      console.error("Erro ao criar assinatura", error);
      alert("Não foi possível guardar a assinatura. Tente novamente.");
    }
  }

  const daysOptions = Array.from({ length: 31 }, (_, i) => ({
    label: `Dia ${i + 1}`,
    value: String(i + 1),
  }));

  const categoryOptions = filteredCategories.map((c) => ({
    label: c.name,
    value: c.id,
  }));

  const paymentOptions =
    paymentMethod === "account"
      ? (accounts as Account[]).map((a) => ({ label: a.name, value: a.id }))
      : (cards as Card[]).map((c) => ({ label: c.name, value: c.id }));

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} size="md">
        <ModalHeader title="Nova Assinatura Recorrente" onClose={onClose} />

        <form id="create-subscription-form" onSubmit={handleSubmit(onSubmit)}>
          <input type="hidden" {...register("account_id")} />
          <input type="hidden" {...register("card_id")} />

          <ModalBody className="p-6 space-y-5 overflow-y-auto max-h-[70vh] overscroll-contain">
            <p className="text-xs font-medium text-muted -mt-2">
              Monitorize os seus serviços fixos e cobranças automáticas
            </p>

            <div>
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                Nome do Serviço *
              </label>
              <input
                type="text"
                placeholder="Ex: Netflix, Spotify, Internet..."
                {...register("title")}
                className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm font-medium shadow-2xs"
              />
              {errors.title && (
                <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                  {errors.title.message}
                </span>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1.5 pl-1">
                  <DollarSign size={13} className="text-muted" />
                  <span>Valor (R$) *</span>
                </label>
                <input
                  type="number"
                  step="0.01"
                  placeholder="0,00"
                  {...register("amount", { valueAsNumber: true })}
                  className="w-full rounded-xl border border-subtle/30 px-4 py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-base font-extrabold shadow-2xs tracking-tight"
                />
                {errors.amount && (
                  <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                    {errors.amount.message}
                  </span>
                )}
              </div>

              <div className="space-y-1.5">
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                  <CalendarDays size={13} className="text-muted" />
                  <span>Dia de Vencimento *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsDueDayPickerOpen(true)}
                  className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer ${
                    currentDueDay ? "text-primary" : "text-muted"
                  }`}
                >
                  <span className="truncate">
                    {currentDueDay
                      ? `Dia ${currentDueDay}`
                      : "Selecionar dia..."}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
                {errors.due_day && (
                  <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                    {errors.due_day.message}
                  </span>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                  <Tag size={13} className="text-muted" />
                  <span>Categoria *</span>
                </label>
                <button
                  type="button"
                  onClick={() => setIsCategoryPickerOpen(true)}
                  className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer ${
                    currentCategoryId ? "text-primary" : "text-muted"
                  }`}
                >
                  <span className="truncate">{selectedCategoryName}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
                {errors.category_id && (
                  <span className="text-danger text-xs font-semibold mt-1 pl-1 block">
                    {errors.category_id.message}
                  </span>
                )}
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                  Periodicidade
                </label>
                <button
                  type="button"
                  onClick={() => setIsFrequencyPickerOpen(true)}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer text-primary"
                >
                  <span>
                    {currentFrequency === "yearly" ? "Anual" : "Mensal"}
                  </span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
            </div>

            <div className="pt-2 border-t border-subtle/20 space-y-3">
              <label className="block text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                Fonte de Cobrança Padrão
              </label>

              <div className="grid grid-cols-2 gap-3 p-1 bg-elevated/60 rounded-2xl border border-subtle/20">
                <label className="flex-1 cursor-pointer">
                  <input
                    type="radio"
                    value="account"
                    {...register("payment_method")}
                    className="peer sr-only"
                    onChange={(e) => {
                      setValue(
                        "payment_method",
                        e.target.value as "account" | "card",
                      );
                      setValue("card_id", undefined);
                    }}
                  />
                  <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-primary peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all">
                    <Building
                      size={16}
                      className={
                        paymentMethod === "account"
                          ? "text-brand"
                          : "text-muted"
                      }
                    />
                    <span>Conta Bancária</span>
                  </div>
                </label>
                <label className="flex-1 cursor-pointer">
                  <input
                    type="radio"
                    value="card"
                    {...register("payment_method")}
                    className="peer sr-only"
                    onChange={(e) => {
                      setValue(
                        "payment_method",
                        e.target.value as "account" | "card",
                      );
                      setValue("account_id", undefined);
                    }}
                  />
                  <div className="flex items-center justify-center gap-2 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-muted peer-checked:bg-surface peer-checked:text-primary peer-checked:shadow-2xs peer-checked:border peer-checked:border-subtle/30 transition-all">
                    <CreditCard
                      size={16}
                      className={
                        paymentMethod === "card" ? "text-brand" : "text-muted"
                      }
                    />
                    <span>Cartão de Crédito</span>
                  </div>
                </label>
              </div>

              <div className="pt-1">
                <button
                  type="button"
                  onClick={() => setIsPaymentPickerOpen(true)}
                  className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border border-subtle/30 bg-elevated/40 hover:bg-surface transition-all text-sm font-semibold shadow-2xs cursor-pointer ${
                    (paymentMethod === "account" && currentAccountId) ||
                    (paymentMethod === "card" && currentCardId)
                      ? "text-primary"
                      : "text-muted"
                  }`}
                >
                  <span className="truncate">{selectedPaymentName}</span>
                  <ChevronDown size={16} className="text-muted shrink-0" />
                </button>
              </div>
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
                disabled={isPending}
                form="create-subscription-form"
                className="flex-1 px-4 py-3 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer flex items-center justify-center"
              >
                {isPending ? (
                  <span className="animate-pulse">A Guardar...</span>
                ) : (
                  "Guardar Assinatura"
                )}
              </button>
            </div>
          </ModalFooter>
        </form>
      </Modal>

      <PickerModal
        isOpen={isCategoryPickerOpen}
        onClose={() => setIsCategoryPickerOpen(false)}
        title="Selecione a Categoria"
        options={categoryOptions}
        selectedValue={currentCategoryId || ""}
        onSelect={(val) => {
          setValue("category_id", String(val), { shouldValidate: true });
          setIsCategoryPickerOpen(false);
        }}
      />

      <PickerModal
        isOpen={isFrequencyPickerOpen}
        onClose={() => setIsFrequencyPickerOpen(false)}
        title="Periodicidade"
        options={[
          { label: "Mensal", value: "monthly" },
          { label: "Anual", value: "yearly" },
        ]}
        selectedValue={currentFrequency || ""}
        onSelect={(val) => {
          setValue("frequency", String(val) as "monthly" | "yearly", {
            shouldValidate: true,
          });
          setIsFrequencyPickerOpen(false);
        }}
      />

      <PickerModal
        isOpen={isDueDayPickerOpen}
        onClose={() => setIsDueDayPickerOpen(false)}
        title="Dia de Vencimento"
        options={daysOptions}
        selectedValue={currentDueDay ? String(currentDueDay) : ""}
        onSelect={(val) => {
          setValue("due_day", Number(val), { shouldValidate: true });
          setIsDueDayPickerOpen(false);
        }}
      />

      <PickerModal
        isOpen={isPaymentPickerOpen}
        onClose={() => setIsPaymentPickerOpen(false)}
        title={
          paymentMethod === "account" ? "Selecionar Conta" : "Selecionar Cartão"
        }
        options={paymentOptions}
        selectedValue={
          paymentMethod === "account"
            ? currentAccountId || ""
            : currentCardId || ""
        }
        onSelect={(val) => {
          // RASTREADOR 1: Ver o que foi clicado no Modal
          console.log("CONTA ESCOLHIDA", val);

          if (paymentMethod === "account") {
            setValue("account_id", String(val), {
              shouldDirty: true,
              shouldTouch: true,
              shouldValidate: true,
            });
            setValue("card_id", undefined);
          } else {
            setValue("card_id", String(val), {
              shouldDirty: true,
              shouldTouch: true,
              shouldValidate: true,
            });
            setValue("account_id", undefined);
          }
          setIsPaymentPickerOpen(false);

          // RASTREADOR 3: Ver o que ficou na memória
          console.log(getValues());
        }}
      />
    </>
  );
}
