import { useState, useEffect } from 'react';
import type { WeekData, Meal } from './types';

const STORAGE_KEY = 'veckans-mat-data';

function loadData(): WeekData {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function saveData(data: WeekData): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function useWeekData() {
  const [data, setData] = useState<WeekData>(loadData);

  useEffect(() => {
    saveData(data);
  }, [data]);

  function setMeal(date: string, meal: Meal) {
    setData((prev) => ({ ...prev, [date]: meal }));
  }

  function removeMeal(date: string) {
    setData((prev) => {
      const next = { ...prev };
      delete next[date];
      return next;
    });
  }

  return { data, setMeal, removeMeal };
}
