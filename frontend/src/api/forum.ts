import { z } from 'zod';
import { forumTopicSchema, forumPostSchema } from '../types/core';
import { core, id } from './axios';
import type { ForumTopicRequest, ForumPostRequest, PostReactionRequest } from '../types/requests';
export const forumApi = {
  topics: async (courseId: number, signal?: AbortSignal) => z.array(forumTopicSchema).parse(await core({ url: `/api/v1/forum/courses/${id(courseId)}/topics`, signal })),
  topic: async (topicId: number, signal?: AbortSignal) => forumTopicSchema.parse(await core({ url: `/api/v1/forum/topics/${id(topicId)}`, signal })),
  createTopic: async (courseId: number, data: ForumTopicRequest) => forumTopicSchema.parse(await core({ method: 'POST', url: `/api/v1/forum/courses/${id(courseId)}/topics`, data })),
  createPost: async (topicId: number, data: ForumPostRequest) => forumPostSchema.parse(await core({ method: 'POST', url: `/api/v1/forum/topics/${id(topicId)}/posts`, data })),
  react: (postId: number, data: PostReactionRequest) => core<void>({ method: 'POST', url: `/api/v1/forum/posts/${id(postId)}/reactions`, data }),
  removeReaction: (postId: number) => core<void>({ method: 'DELETE', url: `/api/v1/forum/posts/${id(postId)}/reactions` }),
};
