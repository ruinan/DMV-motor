import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api-client";

/** An exam the learner can prepare for = a (state × license type) bank. */
export type Exam = {
  id: string;
  state_code: string;
  license_class: string;
  name: string; // already localized by the API per ?language
};

/**
 * The exam catalog. Public endpoint — works signed-in or not (the picker is
 * shown before onboarding too). Localized server-side via ?language.
 */
export function useExams(lang: "en" | "zh") {
  return useQuery({
    queryKey: ["exams", lang],
    queryFn: async () => {
      const data = await apiFetch<{ exams: Exam[] }>(
        `/api/v1/exams?language=${lang}`,
      );
      return data.exams;
    },
    staleTime: 5 * 60_000, // catalog rarely changes
  });
}
