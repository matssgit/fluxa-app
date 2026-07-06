import React from "react";
import { Card, Skeleton } from "../ui";

export function DashboardSkeleton() {
  return (
    <div className="w-full pb-16 min-h-screen animate-fade-in">
      <main className="max-w-6xl mx-auto px-6 py-8 space-y-8">
        {/* 1. GRID SUPERIOR: 4 CARDS DE RESUMO (KPIs) */}
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Card
              key={i}
              variant="metric"
              className="flex items-center justify-between h-24"
            >
              <div className="space-y-2 flex-1 mr-4">
                <Skeleton className="h-3 w-20" />
                <Skeleton className="h-7 w-32" />
              </div>
              <Skeleton className="h-10 w-10 rounded-xl shrink-0" />
            </Card>
          ))}
        </section>

        {/* 2. GRID CENTRAL: HISTÓRICO + COMPROMISSOS */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* HISTÓRICO RECENTE (2 Colunas no Desktop) */}
          <div className="lg:col-span-2 flex flex-col">
            <Card variant="default" className="flex-1 flex flex-col space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-1.5">
                  <Skeleton className="h-5 w-36" />
                  <Skeleton className="h-3 w-56" />
                </div>
              </div>
              <div className="space-y-3 flex-1">
                {[1, 2, 3, 4, 5].map((i) => (
                  <div
                    key={i}
                    className="flex items-center justify-between p-3.5 rounded-2xl border border-subtle/40"
                  >
                    <div className="flex items-center gap-3.5 flex-1 mr-4">
                      <Skeleton className="h-9 w-9 rounded-lg shrink-0" />
                      <div className="space-y-1.5 flex-1">
                        <Skeleton className="h-4 w-40" />
                        <Skeleton className="h-3 w-24" />
                      </div>
                    </div>
                    <Skeleton className="h-4 w-20 shrink-0" />
                  </div>
                ))}
              </div>
            </Card>
          </div>

          {/* COMPROMISSOS (1 Coluna no Desktop) */}
          <div className="flex flex-col">
            <Card variant="default" className="flex-1 flex flex-col space-y-6">
              <div className="space-y-1.5">
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-3 w-44" />
              </div>
              <div className="space-y-3 flex-1">
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="p-4 rounded-2xl border border-subtle/40 space-y-3"
                  >
                    <div className="flex items-center gap-3">
                      <Skeleton className="h-9 w-9 rounded-lg shrink-0" />
                      <div className="space-y-1.5 flex-1">
                        <Skeleton className="h-4 w-28" />
                        <Skeleton className="h-3 w-36" />
                      </div>
                    </div>
                    <div className="flex justify-end pt-1">
                      <Skeleton className="h-8 w-20 rounded-lg" />
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </section>
      </main>
    </div>
  );
}
