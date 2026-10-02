CREATE TABLE forum_topics (
                              id BIGSERIAL PRIMARY KEY,
                              course_id BIGINT NOT NULL,
                              author_id BIGINT NOT NULL,
                              title VARCHAR(255) NOT NULL,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_forum_topic_course
                                  FOREIGN KEY (course_id)
                                      REFERENCES courses(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT fk_forum_topic_author
                                  FOREIGN KEY (author_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE
);

CREATE TABLE forum_posts (
                             id BIGSERIAL PRIMARY KEY,
                             topic_id BIGINT NOT NULL,
                             author_id BIGINT NOT NULL,
                             content TEXT NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_forum_post_topic
                                 FOREIGN KEY (topic_id)
                                     REFERENCES forum_topics(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_forum_post_author
                                 FOREIGN KEY (author_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE
);

CREATE TABLE post_reactions (
                                id BIGSERIAL PRIMARY KEY,
                                post_id BIGINT NOT NULL,
                                user_id BIGINT NOT NULL,
                                reaction VARCHAR(30) NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_post_reaction_post
                                    FOREIGN KEY (post_id)
                                        REFERENCES forum_posts(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_post_reaction_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                    ON DELETE CASCADE,

                                CONSTRAINT uk_post_user_reaction
                                    UNIQUE (post_id, user_id)
);

CREATE INDEX idx_forum_topics_course
    ON forum_topics(course_id);

CREATE INDEX idx_forum_posts_topic
    ON forum_posts(topic_id);

CREATE INDEX idx_post_reactions_post
    ON post_reactions(post_id);