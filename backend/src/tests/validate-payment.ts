// Comando: npx tsx src/test/validate-payment.ts
const API_URL = "http://localhost:3333";
const TOKEN =
   "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZTRjMzFkNC03MmFhLTQxODktYTYzNy1iNjg4MjE4YzU5N2IiLCJpYXQiOjE3ODIzMjE0MTYsImV4cCI6MTc4MjkyNjIxNn0.EZ4WmxbMVRTmGkkqDzQkqJYaTHf3XjNIVd5vd0bu_Vs";
const ACCOUNT_ID = "c1111111-1111-1111-1111-111111111111"; // Se usou o ID forjado acima, já está pronto

async function testPayment() {
   console.log("INICIANDO TESTE DE PAGAMENTO DE PARCELA...\n");

   try {
      // 1. O script busca todas as parcelas sozinho
      const res = await fetch(`${API_URL}/credit/installments`, {
         headers: { Authorization: `Bearer ${TOKEN}` },
      });
      const data = await res.json();

      // 2. Acha a primeira parcela que ainda está "pending"
      const pendingInstallment = data.installments.find(
         (i: any) => i.status === "pending",
      );

      if (!pendingInstallment) {
         throw new Error(
            "Nenhuma parcela pendente encontrada! Rode o teste anterior para gerar compras.",
         );
      }

      console.log(
         `[1/2] Parcela pendente encontrada! ID: ${pendingInstallment.id}`,
      );
      console.log(
         `Valor da parcela a ser paga: R$ ${pendingInstallment.amount}`,
      );

      // 3. Dispara a rota de pagamento enviando o ID da conta
      console.log(
         `\n[2/2] Disparando POST para /credit/installments/${pendingInstallment.id}/pay...`,
      );
      const payRes = await fetch(
         `${API_URL}/credit/installments/${pendingInstallment.id}/pay`,
         {
            method: "POST",
            headers: {
               "Content-Type": "application/json",
               Authorization: `Bearer ${TOKEN}`,
            },
            body: JSON.stringify({ account_id: ACCOUNT_ID }),
         },
      );

      if (payRes.status === 204) {
         console.log(
            "✅ SUCESSO ABSOLUTO! O servidor retornou 204 No Content.",
         );
         console.log("\nA ROTA NÃO DEU ERRO! A transação ACID funcionou.");
         console.log(
            "👉 VÁ PARA O BANCO DE DADOS: Abra a tabela 'transactions' e veja se a sua despesa de R$ -33.33 está lá com o título certinho!",
         );
      } else {
         const errorText = await payRes.text();
         throw new Error(
            `Falha no pagamento. Status: ${payRes.status} - Motivo: ${errorText}`,
         );
      }
   } catch (error: any) {
      console.error("\n❌ ERRO NO SCRIPT:", error.message);
   }
}

testPayment();
