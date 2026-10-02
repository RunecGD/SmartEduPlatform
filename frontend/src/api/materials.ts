import { materialSchema } from '../types/core';
import { core, id } from './axios';
import { z } from 'zod';
export const materialsApi = {
  upload: async (lessonId: number, file: File) => {
    const data = new FormData(); data.append('file', file);
    return materialSchema.parse(await core({ method: 'POST', url: `/api/v1/material/${id(lessonId)}`, data }));
  },
  download: async (materialId: number) => z.object({ url: z.string().url() }).parse(await core({ url: `/api/v1/material/materials/${id(materialId)}/download` })),
};
