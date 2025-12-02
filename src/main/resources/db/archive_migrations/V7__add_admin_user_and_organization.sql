
INSERT INTO organizations (
    created_at,
    updated_at,
    contact_email,
    description,
    logo_url,
    name,
    phone,
    slug,
    website
) 
SELECT 
    NOW(),
    NOW(),
    'moveltrack@gmail.com',
    'Sistema de gestão de eventos esportivos',
    NULL,
    'Moveltrack Sistemas',
    '+55 11 99999-9999',
    'moveltrack-sistemas',
    'https://moveltrack.com.br'
WHERE NOT EXISTS (
    SELECT 1 FROM organizations WHERE slug = 'moveltrack-sistemas'
);

INSERT INTO users (
    id,
    created_at,
    updated_at,
    email,
    enabled,
    password,
    role,
    username,
    name,
    organization_id
) 
SELECT 
    gen_random_uuid(),
    NOW(),
    NOW(),
    'moveltrack@gmail.com',
    true,
    '$2a$10$N4AiWD2QehJEQEXokMH5p.REMSM9hpCe7PPU5LdvMK9HF03iJ5mMG', -- password: 123456
    'ADMIN',
    'moveltrack@gmail.com',
    'Fábio Barros',
    (SELECT id FROM organizations WHERE slug = 'moveltrack-sistemas')
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'moveltrack@gmail.com'
);