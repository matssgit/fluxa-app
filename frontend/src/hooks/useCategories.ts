import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getCategories, createCategory } from "../services/categories";

export function useCategories() {
   const queryClient = useQueryClient();

   const categoriesQuery = useQuery({
      queryKey: ["categories"],
      queryFn: getCategories,
   });

   const createCategoryMutation = useMutation({
      mutationFn: createCategory,
      onSuccess: () => {
         queryClient.invalidateQueries({ queryKey: ["categories"] });
      },
   });

   return {
      categories: categoriesQuery.data || [],
      isLoading: categoriesQuery.isLoading,
      createCategory: createCategoryMutation.mutateAsync,
      isCreating: createCategoryMutation.isPending,
   };
}
