ALTER TABLE user_push_tokens 
ADD CONSTRAINT uk_user_push_tokens_user_token UNIQUE (user_id, token);
